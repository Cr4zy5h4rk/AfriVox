package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Telechargement d'un dataset par lien signe et expirant. Un lien ({@code versionId.profileId.expiration.signature})
 * est delivre a un utilisateur connecte, puis suivi par un navigateur sans en-tete d'authentification : c'est la
 * signature HMAC qui prouve que le serveur l'a emis, et l'expiration qui limite le partage.
 * La cle de signature derive du secret JWT (separation par un libelle), il n'y a donc pas un secret de plus a gerer.
 */
@Service
@Transactional
public class DatasetDownloadService {

    private static final String ENTITY_NAME = "datasetVersion";

    public record Link(String token, Instant expiresAt) {}

    /** Fichier a servir pour un lien valide. */
    public record Archive(Path file, String filename) {}

    private final ApplicationProperties.Datasets properties;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final DownloadLogRepository downloadLogRepository;
    private final UserProfileRepository profileRepository;
    private final UserProfileService userProfileService;
    private final byte[] signingKey;

    public DatasetDownloadService(
        ApplicationProperties applicationProperties,
        DatasetRepository datasetRepository,
        DatasetVersionRepository versionRepository,
        DownloadLogRepository downloadLogRepository,
        UserProfileRepository profileRepository,
        UserProfileService userProfileService,
        @Value("${jhipster.security.authentication.jwt.base64-secret}") String jwtSecret
    ) {
        this.properties = applicationProperties.getDatasets();
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.downloadLogRepository = downloadLogRepository;
        this.profileRepository = profileRepository;
        this.userProfileService = userProfileService;
        this.signingKey = ("dataset-download:" + jwtSecret).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Delivre un lien pour l'utilisateur connecte. Le plan Starter est gratuit ; Plus et Pro sont payants et les
     * paiements ne sont pas dans le MVP : seul l'admin y accede en attendant.
     */
    // Pas readOnly : getOrCreateCurrent() peut creer le profil (INSERT), interdit dans une transaction en lecture seule.
    public Link createLink(String slug, String label) {
        Dataset dataset = datasetRepository.findOneBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DatasetVersion version = versionRepository
            .findOneByDatasetIdAndLabelAndStatus(dataset.getId(), label, VersionStatus.PUBLISHED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (dataset.getPlan() != DatasetPlan.STARTER && !SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Plan payant : demandez un acces");
        }
        UserProfile profile = userProfileService.getOrCreateCurrent();
        Instant expiresAt = Instant.now().plusSeconds(properties.getLinkTtlSeconds());
        String payload = version.getId() + "." + profile.getId() + "." + expiresAt.getEpochSecond();
        return new Link(payload + "." + sign(payload), expiresAt);
    }

    /** Verifie le lien, enregistre le telechargement et renvoie le fichier ; 404 pour tout lien invalide ou expire. */
    public Archive open(String token, String ip) {
        String[] parts = token.split("\\.");
        if (parts.length != 4) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String payload = parts[0] + "." + parts[1] + "." + parts[2];
        // Comparaison en temps constant : on ne revele pas la signature par le temps de reponse.
        if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.UTF_8), parts[3].getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        long expiresAt;
        long versionId;
        long profileId;
        try {
            versionId = Long.parseLong(parts[0]);
            profileId = Long.parseLong(parts[1]);
            expiresAt = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new ResponseStatusException(HttpStatus.GONE, "Lien expire");
        }

        DatasetVersion version = versionRepository
            .findById(versionId)
            .filter(v -> v.getStatus() == VersionStatus.PUBLISHED && v.getPackagePath() != null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Path root = Path.of(properties.getStorageDir()).toAbsolutePath().normalize();
        Path file = root.resolve(version.getPackagePath()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new BadRequestAlertException("Archive introuvable", ENTITY_NAME, "packagemissing");
        }

        DownloadLog log = new DownloadLog();
        log.setDatasetVersion(version);
        log.setProfile(profileRepository.findById(profileId).orElse(null));
        log.setDownloadedAt(Instant.now());
        log.setIp(ip != null && ip.length() <= 45 ? ip : null);
        downloadLogRepository.save(log);
        return new Archive(file, file.getFileName().toString());
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
