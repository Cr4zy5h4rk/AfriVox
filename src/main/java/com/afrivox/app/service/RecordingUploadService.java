package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.domain.enumeration.ConsentKind;
import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.repository.ConsentRepository;
import com.afrivox.app.repository.PromptRepository;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Envoi d'un enregistrement par un contributeur : controles d'entree, stockage sur disque, ligne en base.
 * Les controles qualite automatiques (silence, bruit, duree reelle) viendront apres : l'enregistrement reste
 * {@code SUBMITTED} / {@code qcStatus = PENDING} jusque-la.
 */
@Service
@Transactional
public class RecordingUploadService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingUploadService.class);

    private static final String ENTITY_NAME = "recording";

    private final ApplicationProperties.Audio audioProperties;

    private final RecordingRepository recordingRepository;

    private final PromptRepository promptRepository;

    private final ConsentRepository consentRepository;

    private final UserProfileService userProfileService;

    public RecordingUploadService(
        ApplicationProperties applicationProperties,
        RecordingRepository recordingRepository,
        PromptRepository promptRepository,
        ConsentRepository consentRepository,
        UserProfileService userProfileService
    ) {
        this.audioProperties = applicationProperties.getAudio();
        this.recordingRepository = recordingRepository;
        this.promptRepository = promptRepository;
        this.consentRepository = consentRepository;
        this.userProfileService = userProfileService;
    }

    /**
     * Enregistre l'audio de l'utilisateur connecte pour une phrase.
     *
     * @param durationMs duree annoncee par le client : bornee ici, mais seulement indicative (le controle qualite la
     *                   recalculera a partir du fichier).
     */
    public Recording upload(Long promptId, int durationMs, byte[] audio) {
        if (audio.length == 0 || audio.length > audioProperties.getMaxBytes()) {
            throw new BadRequestAlertException("Taille du fichier audio invalide", ENTITY_NAME, "invalidsize");
        }
        if (durationMs < audioProperties.getMinDurationMs() || durationMs > audioProperties.getMaxDurationMs()) {
            throw new BadRequestAlertException("Duree hors limites", ENTITY_NAME, "invalidduration");
        }
        String extension = AudioFormat.detectExtension(audio).orElseThrow(() ->
            new BadRequestAlertException("Format audio non accepte", ENTITY_NAME, "invalidformat")
        );

        Prompt prompt = promptRepository
            .findById(promptId)
            .orElseThrow(() -> new BadRequestAlertException("Phrase introuvable", ENTITY_NAME, "promptnotfound"));
        Campaign campaign = prompt.getCampaign();
        if (campaign == null || campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BadRequestAlertException("La campagne n'est pas active", ENTITY_NAME, "campaigninactive");
        }

        UserProfile profile = userProfileService.getOrCreateCurrent();
        // Pas d'enregistrement sans consentement de collecte encore actif (un retrait est donc respecte).
        if (consentRepository.findFirstByProfileIdAndKindAndRevokedAtIsNull(profile.getId(), ConsentKind.COLLECTION).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Consentement de collecte requis");
        }

        String sha256 = sha256Hex(audio);
        if (recordingRepository.existsByContributorIdAndSha256(profile.getId(), sha256)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet enregistrement a deja ete envoye");
        }

        String relativePath = store(audio, extension);

        Recording recording = new Recording();
        recording.setPrompt(prompt);
        recording.setContributor(profile);
        recording.setAudioPath(relativePath);
        recording.setDurationMs(durationMs);
        recording.setSha256(sha256);
        recording.setQcStatus(QcStatus.PENDING);
        recording.setStatus(RecordingStatus.SUBMITTED);
        recording.setCreatedAt(Instant.now());
        return recordingRepository.save(recording);
    }

    /** Enregistrements de l'utilisateur connecte, pagines. */
    @Transactional(readOnly = true)
    public Page<Recording> findMine(Pageable pageable) {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        return recordingRepository.findAllByContributorId(profile.getId(), pageable);
    }

    /**
     * Ecrit le fichier sous {@code <dossier>/<annee>/<mois>/<uuid>.<ext>} (nom genere par le serveur, jamais celui du
     * client) et renvoie le chemin relatif au dossier de stockage.
     */
    private String store(byte[] audio, String extension) {
        Path root = Path.of(audioProperties.getStorageDir()).toAbsolutePath().normalize();
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        String relative = "%d/%02d/%s.%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalStateException("Chemin de stockage invalide");
        }
        try {
            Files.createDirectories(target.getParent());
            // Ecriture dans un fichier temporaire puis renommage : jamais de fichier a moitie ecrit.
            Path tmp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(tmp, audio);
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            LOG.error("Ecriture du fichier audio impossible", e);
            throw new UncheckedIOException(e);
        }
        return relative;
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
