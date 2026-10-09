package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publication d'une version de dataset : on fige la liste des enregistrements eligibles, on les range dans une archive
 * {@code .zip} et on enregistre son empreinte SHA-256. Une version publiee n'est plus jamais modifiee : une correction
 * ou un retrait de consentement donne une nouvelle version.
 * <p>
 * Contenu de l'archive : {@code audio/}, {@code manifest.tsv}, {@code splits/} (train, dev, test), {@code DATASHEET.md}
 * (fiche), {@code LICENSE.txt} et {@code checksums.sha256} (empreinte de chaque autre fichier, format {@code sha256sum}).
 */
@Service
@Transactional
public class DatasetPackagingService {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetPackagingService.class);

    private static final String ENTITY_NAME = "datasetVersion";

    private static final double MS_PER_HOUR = 3_600_000d;

    private static final String MANIFEST_HEADER = "id\tfile\tspeaker\tduration_ms\ttext\n";

    /** Part des locuteurs mise de cote pour la mise au point (dev) et pour l'evaluation (test). */
    private static final double HOLDOUT_SHARE = 0.10;

    /** En dessous de ce nombre de locuteurs, un decoupage separe n'aurait pas de sens : tout va en train. */
    private static final int MIN_SPEAKERS_FOR_SPLITS = 3;

    private enum Split {
        TRAIN,
        DEV,
        TEST;

        String fileName() {
            return "splits/" + name().toLowerCase() + ".tsv";
        }
    }

    /** Une ligne du manifeste : un enregistrement, son fichier dans l'archive et son locuteur pseudonymise. */
    private record Row(Recording recording, String name, String speaker, String text, Split split) {
        String line() {
            return "%d\t%s\t%s\t%d\t%s\n".formatted(recording.getId(), name, speaker, durationMs(), text);
        }

        long durationMs() {
            return recording.getDurationMs() == null ? 0 : recording.getDurationMs();
        }
    }

    private final ApplicationProperties.Audio audioProperties;
    private final ApplicationProperties.Datasets datasetProperties;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final RecordingRepository recordingRepository;

    public DatasetPackagingService(
        ApplicationProperties applicationProperties,
        DatasetRepository datasetRepository,
        DatasetVersionRepository versionRepository,
        RecordingRepository recordingRepository
    ) {
        this.audioProperties = applicationProperties.getAudio();
        this.datasetProperties = applicationProperties.getDatasets();
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.recordingRepository = recordingRepository;
    }

    public DatasetVersion publish(Long datasetId, String label, LicenseKind license, Integer pricePerHourFcfa) {
        Dataset dataset = datasetRepository
            .findById(datasetId)
            .orElseThrow(() -> new BadRequestAlertException("Dataset introuvable", ENTITY_NAME, "datasetnotfound"));
        if (dataset.getLanguage() == null) {
            throw new BadRequestAlertException("Le dataset n'a pas de langue", ENTITY_NAME, "nolanguage");
        }
        if (versionRepository.existsByDatasetIdAndLabel(datasetId, label)) {
            throw new BadRequestAlertException("Ce numero de version existe deja", ENTITY_NAME, "versionexists");
        }
        List<Recording> recordings = recordingRepository.findEligibleForDataset(dataset.getLanguage().getId());
        if (recordings.isEmpty()) {
            throw new BadRequestAlertException("Aucun enregistrement valide et consenti pour cette langue", ENTITY_NAME, "norecordings");
        }

        Path root = Path.of(datasetProperties.getStorageDir()).toAbsolutePath().normalize();
        String relative = dataset.getSlug() + "/" + dataset.getSlug() + "-" + label + ".zip";
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestAlertException("Nom de version invalide", ENTITY_NAME, "invalidlabel");
        }

        long totalMs = recordings
            .stream()
            .mapToLong(r -> r.getDurationMs() == null ? 0 : r.getDurationMs())
            .sum();
        long speakers = recordings
            .stream()
            .map(r -> r.getContributor().getId())
            .distinct()
            .count();
        List<Row> rows = buildRows(dataset, label, recordings);
        String sha256 = writeArchive(target, dataset, label, license, rows);

        DatasetVersion version = new DatasetVersion();
        version.setDataset(dataset);
        version.setLabel(label);
        version.setLicense(license);
        version.setPricePerHourFcfa(pricePerHourFcfa);
        version.setHours((float) (totalMs / MS_PER_HOUR));
        version.setSpeakers((int) speakers);
        // Pas de note de qualite : les controles automatiques ne sont pas encore calcules, on n'invente rien.
        version.setStatus(VersionStatus.PUBLISHED);
        version.setPackagePath(relative);
        version.setSha256(sha256);
        version.setCreatedAt(Instant.now());
        version.setRecordings(new java.util.HashSet<>(recordings));
        LOG.info(
            "Version {} du dataset {} publiee : {} enregistrements, {} locuteurs",
            label,
            dataset.getSlug(),
            recordings.size(),
            speakers
        );
        return versionRepository.save(version);
    }

    /**
     * Prepare les lignes du manifeste : nom du fichier, locuteur pseudonymise (spk001...) et decoupage.
     * Le jeu de donnees ne revele aucun compte.
     */
    private List<Row> buildRows(Dataset dataset, String label, List<Recording> recordings) {
        Map<Long, String> pseudonyms = new LinkedHashMap<>();
        for (Recording r : recordings) {
            pseudonyms.computeIfAbsent(r.getContributor().getId(), id -> "spk%03d".formatted(pseudonyms.size() + 1));
        }
        Map<Long, Split> splits = assignSplits(pseudonyms.keySet().stream().toList(), dataset.getSlug() + "/" + label);

        List<Row> rows = new ArrayList<>(recordings.size());
        for (Recording r : recordings) {
            String extension = r.getAudioPath().substring(r.getAudioPath().lastIndexOf('.') + 1);
            String text = r.getTranscript() != null && !r.getTranscript().isBlank() ? r.getTranscript() : r.getPrompt().getContent();
            rows.add(
                new Row(
                    r,
                    "audio/" + r.getId() + "." + extension,
                    pseudonyms.get(r.getContributor().getId()),
                    text.replaceAll("[\\t\\r\\n]+", " "),
                    splits.get(r.getContributor().getId())
                )
            );
        }
        return rows;
    }

    /**
     * Decoupage par locuteur (jamais par enregistrement) : un locuteur du jeu de test n'apparait ni en train ni en dev,
     * sinon le modele evalue aurait deja entendu la voix. L'ordre des locuteurs est melange de facon deterministe
     * (graine = dataset + version) pour qu'il ne suive pas l'ordre d'inscription, et reste reproductible.
     */
    static Map<Long, Split> assignSplits(List<Long> contributorIds, String seed) {
        Map<Long, Split> result = new HashMap<>();
        if (contributorIds.size() < MIN_SPEAKERS_FOR_SPLITS) {
            contributorIds.forEach(id -> result.put(id, Split.TRAIN));
            return result;
        }
        long mix = seed.hashCode();
        List<Long> ordered = new ArrayList<>(contributorIds);
        ordered.sort(Comparator.comparingLong((Long id) -> (id ^ mix) * 0x9E3779B97F4A7C15L).thenComparing(Comparator.naturalOrder()));
        int holdout = Math.max(1, (int) Math.round(ordered.size() * HOLDOUT_SHARE));
        for (int i = 0; i < ordered.size(); i++) {
            result.put(ordered.get(i), i < holdout ? Split.TEST : i < 2 * holdout ? Split.DEV : Split.TRAIN);
        }
        return result;
    }

    /** Ecrit l'archive dans un fichier temporaire, la renomme, et renvoie son SHA-256. */
    private String writeArchive(Path target, Dataset dataset, String label, LicenseKind license, List<Row> rows) {
        Path audioRoot = Path.of(audioProperties.getStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), "build-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(tmp); ZipOutputStream zip = new ZipOutputStream(out)) {
                    // Empreinte de chaque fichier de l'archive, au format de `sha256sum -c`.
                    StringBuilder checksums = new StringBuilder();
                    for (Row row : rows) {
                        Path audio = audioRoot.resolve(row.recording().getAudioPath()).normalize();
                        if (!audio.startsWith(audioRoot) || !Files.isRegularFile(audio)) {
                            throw new IllegalStateException("Fichier audio manquant pour l'enregistrement " + row.recording().getId());
                        }
                        // Une phrase courte tient en memoire (5 Mo au plus) : on lit une fois pour copier et hacher.
                        addBytes(zip, checksums, row.name(), Files.readAllBytes(audio));
                    }
                    StringBuilder manifest = new StringBuilder(MANIFEST_HEADER);
                    rows.forEach(row -> manifest.append(row.line()));
                    addText(zip, checksums, "manifest.tsv", manifest.toString());
                    for (Split split : Split.values()) {
                        StringBuilder part = new StringBuilder(MANIFEST_HEADER);
                        rows.stream()
                            .filter(row -> row.split() == split)
                            .forEach(row -> part.append(row.line()));
                        addText(zip, checksums, split.fileName(), part.toString());
                    }
                    addText(zip, checksums, "DATASHEET.md", datasheet(dataset, label, license, rows));
                    addText(zip, checksums, "LICENSE.txt", licenseText(license, datasetProperties.getLicenseVersion()));
                    addText(zip, null, "checksums.sha256", checksums.toString());
                }
                String sha256 = sha256Hex(tmp);
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                return sha256;
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            LOG.error("Ecriture de l'archive impossible", e);
            throw new UncheckedIOException(e);
        }
    }

    private static void addText(ZipOutputStream zip, StringBuilder checksums, String name, String content) throws IOException {
        addBytes(zip, checksums, name, content.getBytes(StandardCharsets.UTF_8));
    }

    /** Ajoute un fichier a l'archive et, si {@code checksums} n'est pas nul, y note son empreinte. */
    private static void addBytes(ZipOutputStream zip, StringBuilder checksums, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
        if (checksums != null) {
            checksums.append(sha256Hex(bytes)).append("  ").append(name).append('\n');
        }
    }

    /** Fiche du dataset. Tout ce qui y figure est calcule a partir des enregistrements, rien n'est estime. */
    private static String datasheet(Dataset dataset, String label, LicenseKind license, List<Row> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(dataset.getTitle()).append(" - version ").append(label).append("\n\n");
        if (dataset.getDescription() != null && !dataset.getDescription().isBlank()) {
            sb.append(dataset.getDescription()).append("\n\n");
        }
        long totalMs = rows.stream().mapToLong(Row::durationMs).sum();
        sb.append("## Resume\n\n");
        sb.append("- Langue : ").append(dataset.getLanguage().getName()).append('\n');
        if (dataset.getDomain() != null) {
            sb.append("- Domaine : ").append(dataset.getDomain()).append('\n');
        }
        sb.append("- Licence : ").append(license).append(" (voir LICENSE.txt)\n");
        sb.append("- Enregistrements : ").append(rows.size()).append('\n');
        sb.append("- Duree totale : %.2f h%n".formatted(totalMs / MS_PER_HOUR));
        sb.append("- Locuteurs : ").append(rows.stream().map(Row::speaker).distinct().count()).append('\n');
        sb.append("- Qualite : non evaluee (les controles automatiques ne sont pas encore calcules)\n\n");

        sb.append("## Decoupage\n\n");
        sb.append("Le decoupage est fait par locuteur : aucun locuteur n'apparait dans deux parties.\n\n");
        sb.append("| Partie | Enregistrements | Locuteurs | Heures |\n|---|---|---|---|\n");
        for (Split split : Split.values()) {
            List<Row> part = rows
                .stream()
                .filter(row -> row.split() == split)
                .toList();
            sb.append(
                "| %s | %d | %d | %.2f |%n".formatted(
                    split.name().toLowerCase(),
                    part.size(),
                    part.stream().map(Row::speaker).distinct().count(),
                    part.stream().mapToLong(Row::durationMs).sum() / MS_PER_HOUR
                )
            );
        }
        long speakerCount = rows.stream().map(Row::speaker).distinct().count();
        if (speakerCount < MIN_SPEAKERS_FOR_SPLITS) {
            sb.append("\nPeu de locuteurs dans cette version : tout est en train, dev et test sont vides.\n");
        }

        sb.append("\n## Collecte et validation\n\n");
        sb.append("Phrases lues a voix haute par des contributeurs consentants, relues par des pairs qualifies ");
        sb.append("(deux avis concordants). Seuls les enregistrements valides, dont le contributeur a un consentement ");
        sb.append("de collecte actif a la date de publication, sont inclus. Les locuteurs sont pseudonymises.\n\n");

        sb.append("## Contenu de l'archive\n\n");
        sb.append("- `audio/` : un fichier par enregistrement\n");
        sb.append("- `manifest.tsv` : id, file, speaker, duration_ms, text (tabulations)\n");
        sb.append("- `splits/train.tsv`, `splits/dev.tsv`, `splits/test.tsv` : memes colonnes, par partie\n");
        sb.append("- `LICENSE.txt` : conditions d'utilisation\n");
        sb.append("- `checksums.sha256` : empreinte de chaque fichier, verifiable avec `sha256sum -c checksums.sha256`\n\n");

        sb.append("## Limites\n\n");
        sb.append("Phrases lues, pas de la parole spontanee. Le nombre de locuteurs et leur repartition ");
        sb.append("(age, genre, accent) ne sont pas renseignes : ne pas en tirer de conclusion sur des populations.\n");
        return sb.toString();
    }

    /**
     * Texte de la licence. Il resume les droits par type de licence ; la version du texte est celle que l'acheteur
     * accepte a l'achat. Ce texte est a faire relire par l'equipe avant tout usage reel.
     */
    private static String licenseText(LicenseKind license, String textVersion) {
        String scope = switch (license) {
            case PROTOTYPE -> "Essais et demonstrations internes. Aucune mise en production, aucune diffusion des donnees ni de modeles destines au public.";
            case RESEARCH -> "Recherche et enseignement, publications comprises. Aucun usage commercial.";
            case COMMERCIAL -> "Utilisation dans un produit ou un service commercial, y compris l'entrainement de modeles distribues.";
        };
        return (
            "Licence %s (texte %s)%n%n" +
            "Usage autorise : %s%n%n" +
            "Dans tous les cas :%n" +
            "- Les donnees ne sont pas redistribuables, ni en l'etat ni sous forme d'extraits.%n" +
            "- Il est interdit de chercher a identifier les locuteurs.%n" +
            "- Un modele entraine sur ces donnees herite des restrictions de cette licence.%n" +
            "- Un contributeur peut retirer son consentement : ses enregistrements sont alors exclus des versions suivantes.%n"
        ).formatted(license, textVersion, scope);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256Hex(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream stream = new DigestInputStream(in, digest);
            stream.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
