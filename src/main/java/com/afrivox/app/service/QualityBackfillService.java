package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.repository.RecordingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Analyse après coup les enregistrements WAV qui n'ont pas de note (envoyés avant les contrôles, ou importés). Par
 * lots, à la demande de l'admin : rien ne tourne en tâche de fond.
 *
 * <p>Différence avec l'envoi : ici un défaut bloquant ne supprime rien, il marque l'enregistrement {@code FAILED}.
 * Il sort alors de la file de relecture et des futures versions de dataset, mais reste visible et récupérable.
 * Un fichier illisible ou non analysable garde {@code PENDING} et un rapport « unanalyzable » : il n'est pas
 * proposé de nouveau au lot suivant.
 */
@Service
public class QualityBackfillService {

    private static final Logger LOG = LoggerFactory.getLogger(QualityBackfillService.class);

    public static final int MAX_BATCH = 1000;

    public record Result(int analyzed, int passed, int failed, int unanalyzable, long remaining) {}

    /** État global : nombre d'enregistrements par statut de contrôle. */
    public record Summary(long pending, long passed, long failed) {}

    private final RecordingRepository recordingRepository;
    private final ApplicationProperties.Audio audioProperties;

    public QualityBackfillService(RecordingRepository recordingRepository, ApplicationProperties applicationProperties) {
        this.recordingRepository = recordingRepository;
        this.audioProperties = applicationProperties.getAudio();
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        return new Summary(
            recordingRepository.countByQcStatus(QcStatus.PENDING),
            recordingRepository.countByQcStatus(QcStatus.PASSED),
            recordingRepository.countByQcStatus(QcStatus.FAILED)
        );
    }

    /** Analyse au plus {@code limit} enregistrements (1 à {@value #MAX_BATCH}). */
    @Transactional
    public Result run(int limit) {
        int size = Math.max(1, Math.min(MAX_BATCH, limit));
        Path root = Path.of(audioProperties.getStorageDir()).toAbsolutePath().normalize();
        var thresholds = audioProperties.getQc().thresholds();
        List<Recording> batch = recordingRepository.findPendingWav(PageRequest.of(0, size));
        int passed = 0;
        int failed = 0;
        int unanalyzable = 0;
        for (Recording recording : batch) {
            Optional<AudioQualityAnalyzer.Report> report = read(root, recording).flatMap(bytes ->
                AudioQualityAnalyzer.analyze(bytes, thresholds, recording.getDurationMs())
            );
            if (report.isEmpty()) {
                recording.setQcReport("{\"issues\":[\"unanalyzable\"]}");
                unanalyzable++;
            } else {
                AudioQualityAnalyzer.Report r = report.orElseThrow();
                recording.setQcScore(r.score());
                recording.setQcReport(r.toJson());
                if (r.passed()) {
                    recording.setQcStatus(QcStatus.PASSED);
                    passed++;
                } else {
                    recording.setQcStatus(QcStatus.FAILED);
                    failed++;
                }
            }
        }
        recordingRepository.saveAll(batch);
        long remaining = batch.size() < size ? 0 : recordingRepository.findPendingWav(PageRequest.of(0, 1)).size();
        LOG.info("Controle qualite : {} analyses ({} valides, {} refuses, {} illisibles)", batch.size(), passed, failed, unanalyzable);
        return new Result(batch.size(), passed, failed, unanalyzable, remaining);
    }

    private static Optional<byte[]> read(Path root, Recording recording) {
        if (recording.getAudioPath() == null) {
            return Optional.empty();
        }
        Path file = root.resolve(recording.getAudioPath()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
