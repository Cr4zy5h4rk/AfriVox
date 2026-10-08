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
import java.util.HashMap;
import java.util.HexFormat;
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
 * {@code .zip} (audios + manifeste) et on enregistre son empreinte SHA-256. Une version publiee n'est plus jamais
 * modifiee : une correction ou un retrait de consentement donne une nouvelle version.
 */
@Service
@Transactional
public class DatasetPackagingService {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetPackagingService.class);

    private static final String ENTITY_NAME = "datasetVersion";

    private static final double MS_PER_HOUR = 3_600_000d;

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
        String sha256 = writeArchive(target, dataset, label, license, recordings);

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

    /** Ecrit l'archive dans un fichier temporaire, la renomme, et renvoie son SHA-256. */
    private String writeArchive(Path target, Dataset dataset, String label, LicenseKind license, List<Recording> recordings) {
        Path audioRoot = Path.of(audioProperties.getStorageDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), "build-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(tmp); ZipOutputStream zip = new ZipOutputStream(out)) {
                    StringBuilder manifest = new StringBuilder("id\tfile\tspeaker\tduration_ms\ttext\n");
                    // Les locuteurs sont pseudonymises (spk001...) : le jeu de donnees ne revele aucun compte.
                    Map<Long, String> pseudonyms = new HashMap<>();
                    for (Recording r : recordings) {
                        Path audio = audioRoot.resolve(r.getAudioPath()).normalize();
                        if (!audio.startsWith(audioRoot) || !Files.isRegularFile(audio)) {
                            throw new IllegalStateException("Fichier audio manquant pour l'enregistrement " + r.getId());
                        }
                        String extension = r.getAudioPath().substring(r.getAudioPath().lastIndexOf('.') + 1);
                        String name = "audio/" + r.getId() + "." + extension;
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(audio, zip);
                        zip.closeEntry();

                        String speaker = pseudonyms.computeIfAbsent(r.getContributor().getId(), id ->
                            "spk%03d".formatted(pseudonyms.size() + 1)
                        );
                        String text =
                            r.getTranscript() != null && !r.getTranscript().isBlank() ? r.getTranscript() : r.getPrompt().getContent();
                        manifest
                            .append(r.getId())
                            .append('\t')
                            .append(name)
                            .append('\t')
                            .append(speaker)
                            .append('\t')
                            .append(r.getDurationMs())
                            .append('\t')
                            .append(text.replaceAll("[\\t\\r\\n]+", " "))
                            .append('\n');
                    }
                    addText(zip, "manifest.tsv", manifest.toString());
                    addText(zip, "README.txt", readme(dataset, label, license, recordings.size()));
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

    private static void addText(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String readme(Dataset dataset, String label, LicenseKind license, int count) {
        return (
            "%s - version %s%n%nEnregistrements : %d%nLicence : %s%nColonnes de manifest.tsv : id, file, speaker (pseudonyme), duration_ms, text%n%n" +
            "Ces donnees ont ete collectees avec le consentement des contributeurs. Respectez la licence ci-dessus ;%n" +
            "un modele entraine sur ce jeu de donnees herite de ses restrictions.%n"
        ).formatted(dataset.getTitle(), label, count, license);
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
