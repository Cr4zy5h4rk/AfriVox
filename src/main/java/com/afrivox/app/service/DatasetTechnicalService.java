package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vue technique d'une version publiée, à la manière de la page d'un dataset sur Hugging Face : statistiques, découpages,
 * format audio, contenu de l'archive. <b>Tout est lu dans l'archive publiée</b> (manifeste, découpages, en-têtes WAV),
 * donc ce qui est affiché est ce que l'acheteur télécharge, pas une estimation. Rien n'est stocké en double.
 */
@Service
@Transactional(readOnly = true)
public class DatasetTechnicalService {

    /** Bornes (en secondes) des tranches de durée de l'histogramme ; la dernière tranche est ouverte. */
    private static final double[] DURATION_EDGES_SEC = { 0, 2, 4, 6, 8, 10, 15, 20, 30 };

    /** Nombre maximal d'en-têtes audio lus pour décrire le format (échantillon réparti sur l'archive). */
    private static final int MAX_AUDIO_HEADERS = 200;

    private static final int MAX_PREVIEW_ROWS = 10;
    private static final String MANIFEST = "manifest.tsv";

    public record Bucket(double fromSec, Double toSec, long count) {}

    public record TextStats(double meanChars, double meanWords, long totalWords, long vocabulary) {}

    public record Stats(
        long recordings,
        long speakers,
        double hours,
        double minSec,
        double meanSec,
        double medianSec,
        double maxSec,
        long minPerSpeaker,
        double medianPerSpeaker,
        long maxPerSpeaker,
        List<Bucket> durationHistogram,
        TextStats text
    ) {}

    public record SplitStat(String name, long recordings, long speakers, double hours) {}

    public record AudioFormatView(
        String container,
        Integer sampleRateHz,
        Integer channels,
        Integer bitsPerSample,
        String encoding,
        long filesInspected
    ) {}

    public record ArchiveFile(String path, long sizeBytes) {}

    public record Technical(
        String slug,
        String version,
        LicenseKind license,
        Instant publishedAt,
        String sha256,
        long archiveBytes,
        Stats stats,
        List<SplitStat> splits,
        /** Un format par combinaison distincte trouvée dans l'échantillon d'en-têtes lus. */
        List<AudioFormatView> audioFormats,
        List<String> columns,
        List<ArchiveFile> files,
        long audioFiles,
        long audioBytes,
        List<ModelSummary> models
    ) {}

    /** {@code audio} : un clip est ecoutable pour cette ligne (interrupteur du dataset actif et ligne de l'echantillon). */
    public record PreviewRow(long id, String file, String speaker, long durationMs, String text, String split, boolean audio) {}

    /** Clip audio d'un echantillon, tel qu'il est dans l'archive publiee. */
    public record AudioClip(byte[] data, String contentType) {}

    /** Nombre de clips ecoutables par dataset : assez pour juger la qualite, trop peu pour reconstituer le jeu. */
    static final int PREVIEW_AUDIO_CLIPS = 5;

    /** Un clip de plus de 5 Mo n'est pas un extrait : on ne le sert pas. */
    private static final long MAX_CLIP_BYTES = 5L * 1024 * 1024;

    /** Lignes de l'apercu et decoupage de chacune ; le test n'entre jamais dans l'echantillon audio. */
    private record PreviewData(List<ManifestRow> head, Map<Long, String> splitOf) {
        /** Les {@value DatasetTechnicalService#PREVIEW_AUDIO_CLIPS} premieres lignes hors test, dans l'ordre du manifeste. */
        Set<Long> sampleIds() {
            Set<Long> ids = new java.util.LinkedHashSet<>();
            for (ManifestRow r : head) {
                if (ids.size() >= PREVIEW_AUDIO_CLIPS) {
                    break;
                }
                if (!"test".equals(splitOf.getOrDefault(r.id(), "train"))) {
                    ids.add(r.id());
                }
            }
            return ids;
        }
    }

    private final ApplicationProperties.Datasets properties;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final MlModelRepository modelRepository;
    private final PublicCatalogService catalog;

    public DatasetTechnicalService(
        ApplicationProperties applicationProperties,
        DatasetRepository datasetRepository,
        DatasetVersionRepository versionRepository,
        MlModelRepository modelRepository,
        PublicCatalogService catalog
    ) {
        this.properties = applicationProperties.getDatasets();
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.modelRepository = modelRepository;
        this.catalog = catalog;
    }

    /** {@code label} vide = dernière version publiée. Vide si le dataset n'a aucune version publiée ou archive. */
    public Optional<Technical> technical(String slug, String label) {
        return findVersion(slug, label).flatMap(v -> {
            Path file = archivePath(v);
            if (file == null) {
                return Optional.empty();
            }
            try (ZipFile zip = new ZipFile(file.toFile())) {
                List<ManifestRow> rows = readManifest(zip, MANIFEST);
                Stats stats = stats(rows);

                List<SplitStat> splits = new ArrayList<>();
                for (String name : List.of("train", "dev", "test")) {
                    List<ManifestRow> part = readManifest(zip, "splits/" + name + ".tsv");
                    double hours = part.stream().mapToLong(ManifestRow::durationMs).sum() / 3_600_000d;
                    splits.add(new SplitStat(name, part.size(), part.stream().map(ManifestRow::speaker).distinct().count(), hours));
                }

                List<ArchiveFile> files = new ArrayList<>();
                long audioFiles = 0;
                long audioBytes = 0;
                List<ZipEntry> audioEntries = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    if (entry.getName().startsWith("audio/")) {
                        audioFiles++;
                        audioBytes += entry.getSize();
                        audioEntries.add(entry);
                    } else {
                        files.add(new ArchiveFile(entry.getName(), entry.getSize()));
                    }
                }
                files.sort(java.util.Comparator.comparing(ArchiveFile::path));

                return Optional.of(
                    new Technical(
                        v.getDataset().getSlug(),
                        v.getLabel(),
                        v.getLicense(),
                        v.getCreatedAt(),
                        v.getSha256(),
                        Files.size(file),
                        stats,
                        splits,
                        audioFormats(zip, audioEntries),
                        List.of("id", "file", "speaker", "duration_ms", "text"),
                        files,
                        audioFiles,
                        audioBytes,
                        modelRepository.findTrainedOnDataset(v.getDataset().getSlug()).stream().map(catalog::toModelSummary).toList()
                    )
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Premières lignes du manifeste (texte et durée), au plus {@value #MAX_PREVIEW_ROWS}. Si l'interrupteur
     * {@code previewAudio} du dataset est actif, {@value #PREVIEW_AUDIO_CLIPS} d'entre elles ont un clip écoutable
     * ({@link #previewAudio}) ; sinon aucun audio n'est exposé. Réservé aux utilisateurs connectés.
     */
    public Optional<List<PreviewRow>> preview(String slug, String label) {
        return findVersion(slug, label).flatMap(v -> {
            Path file = archivePath(v);
            if (file == null) {
                return Optional.empty();
            }
            try (ZipFile zip = new ZipFile(file.toFile())) {
                PreviewData data = readPreview(zip);
                Set<Long> sample = v.getDataset().isPreviewAudio() ? data.sampleIds() : Set.of();
                return Optional.of(
                    data
                        .head()
                        .stream()
                        .map(r ->
                            new PreviewRow(
                                r.id(),
                                r.file(),
                                r.speaker(),
                                r.durationMs(),
                                r.text(),
                                data.splitOf().getOrDefault(r.id(), "train"),
                                sample.contains(r.id())
                            )
                        )
                        .toList()
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Un clip de l'échantillon audio. Vide (404) si l'interrupteur du dataset est coupé, si la ligne n'est pas dans
     * l'échantillon (le client ne peut donc pas demander un autre fichier de l'archive) ou si le fichier manque.
     */
    public Optional<AudioClip> previewAudio(String slug, String label, long id) {
        return findVersion(slug, label)
            .filter(v -> v.getDataset().isPreviewAudio())
            .flatMap(v -> {
                Path file = archivePath(v);
                if (file == null) {
                    return Optional.empty();
                }
                try (ZipFile zip = new ZipFile(file.toFile())) {
                    PreviewData data = readPreview(zip);
                    if (!data.sampleIds().contains(id)) {
                        return Optional.empty();
                    }
                    Optional<ManifestRow> row = data
                        .head()
                        .stream()
                        .filter(r -> r.id() == id)
                        .findFirst();
                    if (row.isEmpty() || !row.get().file().startsWith("audio/")) {
                        return Optional.empty();
                    }
                    ZipEntry entry = zip.getEntry(row.get().file());
                    if (entry == null || entry.isDirectory() || entry.getSize() > MAX_CLIP_BYTES) {
                        return Optional.empty();
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        return Optional.of(new AudioClip(in.readAllBytes(), contentType(entry.getName())));
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }

    private static PreviewData readPreview(ZipFile zip) throws IOException {
        List<ManifestRow> rows = readManifest(zip, MANIFEST);
        List<ManifestRow> head = rows.subList(0, Math.min(MAX_PREVIEW_ROWS, rows.size()));
        Map<Long, String> splitOf = new HashMap<>();
        for (String name : List.of("train", "dev", "test")) {
            for (ManifestRow r : readManifest(zip, "splits/" + name + ".tsv")) {
                splitOf.put(r.id(), name);
            }
        }
        return new PreviewData(head, splitOf);
    }

    private static String contentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }

    private Optional<DatasetVersion> findVersion(String slug, String label) {
        return datasetRepository.findOneBySlug(slug).flatMap(d -> {
            List<DatasetVersion> published = versionRepository.findAllByDatasetIdAndStatusOrderByCreatedAtDesc(
                d.getId(),
                VersionStatus.PUBLISHED
            );
            if (label == null || label.isBlank()) {
                return published.stream().findFirst();
            }
            return published
                .stream()
                .filter(v -> v.getLabel().equals(label))
                .findFirst();
        });
    }

    /** Chemin de l'archive, ou null si elle manque (le dataset reste consultable, sans la partie technique). */
    private Path archivePath(DatasetVersion v) {
        if (v.getPackagePath() == null) {
            return null;
        }
        Path root = Path.of(properties.getStorageDir()).toAbsolutePath().normalize();
        Path file = root.resolve(v.getPackagePath()).normalize();
        return file.startsWith(root) && Files.isRegularFile(file) ? file : null;
    }

    private record ManifestRow(long id, String file, String speaker, long durationMs, String text) {}

    private static List<ManifestRow> readManifest(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) {
            return List.of();
        }
        List<ManifestRow> rows = new ArrayList<>();
        try (InputStream in = zip.getInputStream(entry)) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            boolean header = true;
            for (String line : content.split("\n")) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                String[] cols = line.split("\t", 5);
                if (cols.length < 5) {
                    continue;
                }
                try {
                    rows.add(new ManifestRow(Long.parseLong(cols[0]), cols[1], cols[2], Long.parseLong(cols[3]), cols[4]));
                } catch (NumberFormatException ignored) {
                    // ligne illisible : ignorée plutôt que de fausser les chiffres
                }
            }
        }
        return rows;
    }

    private static Stats stats(List<ManifestRow> rows) {
        if (rows.isEmpty()) {
            return new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), new TextStats(0, 0, 0, 0));
        }
        List<Long> durations = rows.stream().map(ManifestRow::durationMs).sorted().toList();
        long total = durations.stream().mapToLong(Long::longValue).sum();

        long[] counts = new long[DURATION_EDGES_SEC.length];
        for (long ms : durations) {
            double sec = ms / 1000d;
            int bucket = 0;
            for (int i = 0; i < DURATION_EDGES_SEC.length; i++) {
                if (sec >= DURATION_EDGES_SEC[i]) {
                    bucket = i;
                }
            }
            counts[bucket]++;
        }
        List<Bucket> histogram = new ArrayList<>();
        for (int i = 0; i < DURATION_EDGES_SEC.length; i++) {
            Double to = i + 1 < DURATION_EDGES_SEC.length ? DURATION_EDGES_SEC[i + 1] : null;
            histogram.add(new Bucket(DURATION_EDGES_SEC[i], to, counts[i]));
        }

        Map<String, Long> perSpeaker = new LinkedHashMap<>();
        for (ManifestRow r : rows) {
            perSpeaker.merge(r.speaker(), 1L, Long::sum);
        }
        List<Long> perSpeakerSorted = new ArrayList<>(perSpeaker.values());
        Collections.sort(perSpeakerSorted);

        long totalWords = 0;
        long totalChars = 0;
        Set<String> vocabulary = new HashSet<>();
        // Le texte de la partie test est volontairement vide dans l'archive (reference du classement) : ces lignes
        // ne comptent pas dans les moyennes de texte.
        long textRows = 0;
        for (ManifestRow r : rows) {
            if (r.text().isBlank()) {
                continue;
            }
            textRows++;
            totalChars += r.text().codePointCount(0, r.text().length());
            for (String word : r.text().toLowerCase().split("\\s+")) {
                if (!word.isBlank()) {
                    totalWords++;
                    vocabulary.add(word);
                }
            }
        }

        return new Stats(
            rows.size(),
            perSpeaker.size(),
            total / 3_600_000d,
            durations.get(0) / 1000d,
            total / 1000d / rows.size(),
            median(durations) / 1000d,
            durations.get(durations.size() - 1) / 1000d,
            perSpeakerSorted.get(0),
            median(perSpeakerSorted),
            perSpeakerSorted.get(perSpeakerSorted.size() - 1),
            histogram,
            textRows == 0
                ? new TextStats(0, 0, 0, 0)
                : new TextStats((double) totalChars / textRows, (double) totalWords / textRows, totalWords, vocabulary.size())
        );
    }

    private static double median(List<Long> sorted) {
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2d;
    }

    /**
     * Format réel des fichiers audio : lit l'en-tête WAV d'un échantillon réparti des fichiers (au plus
     * {@value #MAX_AUDIO_HEADERS}) et regroupe les combinaisons trouvées. Annonce combien de fichiers ont été vus.
     */
    private static List<AudioFormatView> audioFormats(ZipFile zip, List<ZipEntry> audioEntries) {
        if (audioEntries.isEmpty()) {
            return List.of();
        }
        int step = Math.max(1, audioEntries.size() / MAX_AUDIO_HEADERS);
        Map<String, long[]> found = new LinkedHashMap<>();
        Map<String, AudioFormatView> views = new LinkedHashMap<>();
        TreeSet<Integer> sampled = new TreeSet<>();
        for (int i = 0; i < audioEntries.size() && sampled.size() < MAX_AUDIO_HEADERS; i += step) {
            sampled.add(i);
        }
        for (int index : sampled) {
            ZipEntry entry = audioEntries.get(index);
            byte[] header = new byte[44];
            try (InputStream in = zip.getInputStream(entry)) {
                int read = in.readNBytes(header, 0, header.length);
                AudioFormatView view = read == header.length ? parseWav(header) : null;
                String key =
                    view == null
                        ? "unknown"
                        : view.container() +
                          "/" +
                          view.encoding() +
                          "/" +
                          view.sampleRateHz() +
                          "/" +
                          view.channels() +
                          "/" +
                          view.bitsPerSample();
                found.computeIfAbsent(key, k -> new long[1])[0]++;
                views.putIfAbsent(key, view == null ? new AudioFormatView("unknown", null, null, null, null, 0) : view);
            } catch (IOException e) {
                found.computeIfAbsent("unknown", k -> new long[1])[0]++;
                views.putIfAbsent("unknown", new AudioFormatView("unknown", null, null, null, null, 0));
            }
        }
        List<AudioFormatView> out = new ArrayList<>();
        views.forEach((key, v) ->
            out.add(new AudioFormatView(v.container(), v.sampleRateHz(), v.channels(), v.bitsPerSample(), v.encoding(), found.get(key)[0]))
        );
        return out;
    }

    /** En-tête RIFF/WAVE canonique de 44 octets : format, canaux, fréquence, bits par échantillon. */
    private static AudioFormatView parseWav(byte[] h) {
        boolean riff =
            h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
        // Le bloc "fmt " doit suivre l'en-tête RIFF ; avec un autre bloc d'abord (LIST...), les offsets ne valent plus.
        boolean fmtFirst = h[12] == 'f' && h[13] == 'm' && h[14] == 't' && h[15] == ' ';
        if (!riff || !fmtFirst) {
            return null;
        }
        int format = (h[20] & 0xff) | ((h[21] & 0xff) << 8);
        int channels = (h[22] & 0xff) | ((h[23] & 0xff) << 8);
        int rate = (h[24] & 0xff) | ((h[25] & 0xff) << 8) | ((h[26] & 0xff) << 16) | ((h[27] & 0xff) << 24);
        int bits = (h[34] & 0xff) | ((h[35] & 0xff) << 8);
        String encoding = format == 1 ? "PCM" : format == 3 ? "IEEE float" : "format " + format;
        return new AudioFormatView("WAV", rate, channels, bits, encoding, 0);
    }
}
