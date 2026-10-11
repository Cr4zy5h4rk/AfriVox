package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.DownloadLog;
import com.afrivox.app.domain.Purchase;
import com.afrivox.app.domain.PurchaseLink;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.DownloadLogRepository;
import com.afrivox.app.repository.PurchaseLinkRepository;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Parcours d'achat d'un dataset : devis, acceptation de la licence, paiement simule, puis liens de telechargement
 * lies a l'achat. Un lien expire, est limite en nombre de telechargements, et son emission est plafonnee par jour.
 * Tout passe par le compte connecte : on ne lit jamais l'achat d'un autre.
 */
@Service
@Transactional
public class PurchaseService {

    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    private static final String ENTITY_NAME = "purchase";

    /** Devis : prix total pour une version et une licence. {@code free} vaut vrai pour le plan Starter. */
    public record Quote(
        String slug,
        String versionLabel,
        LicenseKind license,
        Float hours,
        Integer pricePerHourFcfa,
        long totalFcfa,
        boolean free
    ) {}

    public record Link(String token, Instant expiresAt, int maxDownloads) {}

    /**
     * Fichier a servir pour un lien valide. Si {@code personalize} est vrai, on ne sert pas le fichier tel quel mais une
     * copie propre a l'achat ({@link #writePersonalized}), nommee {@code filename}.
     */
    public record Archive(
        Path file,
        String filename,
        boolean personalize,
        Long purchaseId,
        String fingerprint,
        String datasetSlug,
        String versionLabel
    ) {}

    /** Achat vu par son proprietaire. {@code newerVersionLabel} est non nul quand une version plus recente est publiee. */
    public record PurchaseView(
        Long id,
        String datasetSlug,
        String datasetTitle,
        String versionLabel,
        LicenseKind license,
        long priceFcfa,
        String licenseVersion,
        Instant licenseAcceptedAt,
        Instant createdAt,
        String newerVersionLabel
    ) {}

    private final ApplicationProperties.Datasets properties;
    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseLinkRepository linkRepository;
    private final DownloadLogRepository downloadLogRepository;
    private final UserProfileService userProfileService;
    private final byte[] signingKey;

    public PurchaseService(
        ApplicationProperties applicationProperties,
        DatasetRepository datasetRepository,
        DatasetVersionRepository versionRepository,
        PurchaseRepository purchaseRepository,
        PurchaseLinkRepository linkRepository,
        DownloadLogRepository downloadLogRepository,
        UserProfileService userProfileService,
        @Value("${jhipster.security.authentication.jwt.base64-secret}") String jwtSecret
    ) {
        this.properties = applicationProperties.getDatasets();
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.purchaseRepository = purchaseRepository;
        this.linkRepository = linkRepository;
        this.downloadLogRepository = downloadLogRepository;
        this.userProfileService = userProfileService;
        // Libelle propre aux liens d'achat : un jeton d'un autre usage ne peut pas etre rejoue ici.
        this.signingKey = ("purchase-download:" + jwtSecret).getBytes(StandardCharsets.UTF_8);
    }

    /** Prix total d'une version sous une licence. Ne depend d'aucun compte, donc utilisable avant la connexion. */
    @Transactional(readOnly = true)
    public Quote quote(String slug, String label, LicenseKind license) {
        Dataset dataset = datasetRepository.findOneBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DatasetVersion version = publishedVersion(dataset, label);
        return quote(dataset, version, license);
    }

    private Quote quote(Dataset dataset, DatasetVersion version, LicenseKind license) {
        boolean free = dataset.getPlan() == DatasetPlan.STARTER;
        long total = 0;
        if (!free) {
            if (version.getHours() == null || version.getPricePerHourFcfa() == null) {
                throw new BadRequestAlertException("Tarif indisponible pour cette version", ENTITY_NAME, "nopricing");
            }
            total = Math.round(version.getHours() * (double) version.getPricePerHourFcfa() * multiplier(license));
        }
        return new Quote(dataset.getSlug(), version.getLabel(), license, version.getHours(), version.getPricePerHourFcfa(), total, free);
    }

    private double multiplier(LicenseKind license) {
        return switch (license) {
            case PROTOTYPE -> properties.getPrototypeMultiplier();
            case RESEARCH -> properties.getResearchMultiplier();
            case COMMERCIAL -> properties.getCommercialMultiplier();
        };
    }

    /**
     * Cree l'achat. {@code accepted} materialise les cases a cocher : sans elles, rien n'est enregistre.
     * Le paiement est simule (aucun service externe) : il reussit toujours et laisse une reference.
     */
    // Pas readOnly : getOrCreateCurrent() peut creer le profil (INSERT).
    public PurchaseView purchase(String slug, String label, LicenseKind license, boolean accepted) {
        if (!accepted) {
            throw new BadRequestAlertException("La licence doit etre acceptee", ENTITY_NAME, "licensenotaccepted");
        }
        Dataset dataset = datasetRepository.findOneBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DatasetVersion version = publishedVersion(dataset, label);
        Quote quote = quote(dataset, version, license);
        UserProfile profile = userProfileService.getOrCreateCurrent();
        if (purchaseRepository.existsByProfileIdAndDatasetVersionIdAndLicense(profile.getId(), version.getId(), license)) {
            throw new BadRequestAlertException("Vous avez deja achete cette version sous cette licence", ENTITY_NAME, "alreadypurchased");
        }
        Instant now = Instant.now();
        Purchase purchase = new Purchase();
        purchase.setProfile(profile);
        purchase.setDatasetVersion(version);
        purchase.setLicense(license);
        purchase.setPriceFcfa(quote.totalFcfa());
        purchase.setLicenseVersion(properties.getLicenseVersion());
        purchase.setLicenseAcceptedAt(now);
        purchase.setPaymentRef(quote.free() ? null : "SIM-" + UUID.randomUUID());
        purchase.setFingerprint(newFingerprint());
        purchase.setCreatedAt(now);
        try {
            purchase = purchaseRepository.saveAndFlush(purchase);
        } catch (DataIntegrityViolationException e) {
            // Deux requetes simultanees : la contrainte d'unicite de la base tranche.
            throw new BadRequestAlertException("Vous avez deja achete cette version sous cette licence", ENTITY_NAME, "alreadypurchased");
        }
        return view(purchase, dataset);
    }

    public List<PurchaseView> listMine() {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        return purchaseRepository
            .findAllByProfile(profile.getId())
            .stream()
            .map(p -> view(p, p.getDatasetVersion().getDataset()))
            .toList();
    }

    /**
     * Regenere un lien sans repayer. Verifie : l'achat est au compte connecte, la version est toujours publiee
     * (la licence est donc encore valable), et le plafond quotidien n'est pas atteint.
     */
    public Link createLink(Long purchaseId) {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        Purchase purchase = purchaseRepository
            .findOneByIdAndProfile(purchaseId, profile.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        DatasetVersion version = purchase.getDatasetVersion();
        if (version.getStatus() != VersionStatus.PUBLISHED || version.getPackagePath() == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Cette version n'est plus disponible");
        }
        Instant now = Instant.now();
        long recent = linkRepository.countByPurchaseIdAndCreatedAtAfter(purchase.getId(), now.minus(Duration.ofDays(1)));
        if (recent >= properties.getMaxLinksPerDay()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de liens atteinte, reessayez plus tard");
        }
        PurchaseLink link = new PurchaseLink();
        link.setPurchase(purchase);
        link.setCreatedAt(now);
        link.setExpiresAt(now.plusSeconds(properties.getPurchaseLinkTtlSeconds()));
        link.setMaxDownloads(properties.getMaxDownloadsPerLink());
        link = linkRepository.save(link);
        String id = String.valueOf(link.getId());
        return new Link(id + "." + sign(id), link.getExpiresAt(), link.getMaxDownloads());
    }

    /**
     * Verifie le jeton, consomme un telechargement, journalise et renvoie le fichier. Le meme 404 pour tout jeton
     * invalide ; 410 quand le lien est expire ou epuise.
     */
    public Archive open(String token, String ip) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // Comparaison en temps constant : on ne revele pas la signature par le temps de reponse.
        if (!MessageDigest.isEqual(sign(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        long linkId;
        try {
            linkId = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        PurchaseLink link = linkRepository.findById(linkId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Purchase purchase = link.getPurchase();
        DatasetVersion version = purchase.getDatasetVersion();
        if (version.getStatus() != VersionStatus.PUBLISHED || version.getPackagePath() == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Cette version n'est plus disponible");
        }
        Path root = Path.of(properties.getStorageDir()).toAbsolutePath().normalize();
        Path file = root.resolve(version.getPackagePath()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new BadRequestAlertException("Archive introuvable", ENTITY_NAME, "packagemissing");
        }
        // Le fichier est verifie avant de consommer : une archive absente ne doit pas epuiser le lien.
        if (linkRepository.consumeDownload(linkId, Instant.now()) == 0) {
            throw new ResponseStatusException(HttpStatus.GONE, "Lien expire ou epuise");
        }

        DownloadLog log = new DownloadLog();
        log.setDatasetVersion(version);
        log.setProfile(purchase.getProfile());
        log.setDownloadedAt(Instant.now());
        log.setIp(ip != null && ip.length() <= 45 ? ip : null);
        downloadLogRepository.save(log);

        // Les achats faits avant l'empreinte en recoivent une au premier telechargement.
        if (purchase.getFingerprint() == null) {
            purchase.setFingerprint(newFingerprint());
            purchaseRepository.save(purchase);
        }
        Dataset dataset = version.getDataset();
        String filename = properties.isPersonalizeDownloads()
            ? "%s-%s-%d.zip".formatted(dataset.getSlug(), version.getLabel(), purchase.getId())
            : file.getFileName().toString();
        return new Archive(
            file,
            filename,
            properties.isPersonalizeDownloads(),
            purchase.getId(),
            purchase.getFingerprint(),
            dataset.getSlug(),
            version.getLabel()
        );
    }

    /**
     * Ecrit la copie personnalisee de l'acheteur dans {@code out}. Appele apres la transaction de {@link #open} :
     * il n'utilise que ce que l'{@link Archive} contient.
     */
    public void writePersonalized(Archive archive, java.io.OutputStream out) throws java.io.IOException {
        String provenance = (
            "Copie numerotee - AfriVox%n%n" +
            "Dataset : %s, version %s%n" +
            "Achat : n. %d%n%n" +
            "Cette copie est propre a l'achat ci-dessus : les noms de fichiers, l'ordre des lignes et le contenu de " +
            "certains fichiers audio lui sont specifiques. Ils permettent de retrouver l'origine d'une copie diffusee " +
            "sans autorisation, ce que la licence (LICENSE.txt) interdit.%n%n" +
            "Le contenu des donnees (audio, transcriptions, decoupage) est identique pour tous les acheteurs. " +
            "Verifier l'integrite : sha256sum -c checksums.sha256%n"
        ).formatted(archive.datasetSlug(), archive.versionLabel(), archive.purchaseId());
        ArchivePersonalizer.Result result = ArchivePersonalizer.personalize(archive.file(), out, archive.fingerprint(), provenance);
        LOG.info(
            "Copie personnalisee livree : achat {}, {} fichiers audio dont {} marques",
            archive.purchaseId(),
            result.audioFiles(),
            result.watermarked()
        );
    }

    /** Empreinte secrete d'un achat : 128 bits aleatoires (32 caracteres hexadecimaux). */
    private static String newFingerprint() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    private DatasetVersion publishedVersion(Dataset dataset, String label) {
        return versionRepository
            .findOneByDatasetIdAndLabelAndStatus(dataset.getId(), label, VersionStatus.PUBLISHED)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private PurchaseView view(Purchase p, Dataset dataset) {
        DatasetVersion bought = p.getDatasetVersion();
        // Liste triee de la plus recente a la plus ancienne : la premiere est la derniere version publiee.
        String newer = versionRepository
            .findAllByDatasetIdAndStatusOrderByCreatedAtDesc(dataset.getId(), VersionStatus.PUBLISHED)
            .stream()
            .findFirst()
            .filter(latest -> latest.getCreatedAt().isAfter(bought.getCreatedAt()))
            .map(DatasetVersion::getLabel)
            .orElse(null);
        return new PurchaseView(
            p.getId(),
            dataset.getSlug(),
            dataset.getTitle(),
            bought.getLabel(),
            p.getLicense(),
            p.getPriceFcfa(),
            p.getLicenseVersion(),
            p.getLicenseAcceptedAt(),
            p.getCreatedAt(),
            newer
        );
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
