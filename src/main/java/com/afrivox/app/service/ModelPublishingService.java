package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publication de modeles par les developpeurs : chacun ne voit et ne supprime que les siens. */
@Service
@Transactional
public class ModelPublishingService {

    private static final String ENTITY_NAME = "model";

    /** Donnees saisies par le developpeur. */
    public record NewModel(
        String name,
        MlTask task,
        String description,
        String license,
        String url,
        String sha256,
        String languageCode,
        Long trainedOnVersionId,
        List<Long> otherVersionIds,
        LicenseKind usage
    ) {}

    /** Nombre maximal de datasets supplementaires par modele. */
    static final int MAX_OTHER_DATASETS = 10;

    private final MlModelRepository modelRepository;
    private final LanguageRepository languageRepository;
    private final DatasetVersionRepository versionRepository;
    private final UserProfileService userProfileService;
    private final PublicCatalogService catalog;
    private final PurchaseRepository purchaseRepository;

    public ModelPublishingService(
        MlModelRepository modelRepository,
        LanguageRepository languageRepository,
        DatasetVersionRepository versionRepository,
        UserProfileService userProfileService,
        PublicCatalogService catalog,
        PurchaseRepository purchaseRepository
    ) {
        this.purchaseRepository = purchaseRepository;
        this.modelRepository = modelRepository;
        this.languageRepository = languageRepository;
        this.versionRepository = versionRepository;
        this.userProfileService = userProfileService;
        this.catalog = catalog;
    }

    public ModelSummary publish(NewModel input) {
        UserProfile owner = userProfileService.getOrCreateCurrent();

        MlModel model = new MlModel();
        model.setName(input.name().trim());
        model.setTask(input.task());
        model.setDescription(blankToNull(input.description()));
        model.setLicense(blankToNull(input.license()));
        model.setUrl(validateUrl(input.url()));
        model.setSha256(validateSha256(input.sha256()));
        model.setDownloads(0);
        model.setCreatedAt(Instant.now());
        model.setOwner(owner);

        if (input.languageCode() != null && !input.languageCode().isBlank()) {
            Language language = languageRepository
                .findOneByCode(input.languageCode().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestAlertException("Langue inconnue", ENTITY_NAME, "languagenotfound"));
            model.setLanguage(language);
        }
        if (input.trainedOnVersionId() != null) {
            model.setTrainedOn(publishedVersion(input.trainedOnVersionId()));
        }

        // Datasets supplementaires : sans dataset principal on ne saurait pas lequel est lequel ; doublons ignores,
        // et le principal n'est pas repete dans la liste.
        Set<Long> otherIds = new LinkedHashSet<>(input.otherVersionIds() == null ? List.of() : input.otherVersionIds());
        otherIds.remove(input.trainedOnVersionId());
        if (!otherIds.isEmpty() && input.trainedOnVersionId() == null) {
            throw new BadRequestAlertException("Choisissez d'abord le dataset principal", ENTITY_NAME, "primarymissing");
        }
        if (otherIds.size() > MAX_OTHER_DATASETS) {
            throw new BadRequestAlertException("Trop de datasets supplementaires", ENTITY_NAME, "toomanydatasets");
        }
        otherIds.forEach(id -> model.getOtherDatasets().add(publishedVersion(id)));

        applyLicenseInheritance(model, owner, input.usage());
        return catalog.toModelSummary(modelRepository.save(model));
    }

    /**
     * Héritage de licence : l'usage déclaré ne peut pas dépasser la plus restrictive des licences que l'auteur détient
     * sur les datasets cités. Chaque dataset cité doit avoir été acheté par l'auteur (un achat gratuit suffit pour le
     * plan Starter) : sans achat, on ne sait pas ce qu'il a le droit d'en faire.
     */
    private void applyLicenseInheritance(MlModel model, UserProfile owner, LicenseKind declared) {
        Set<DatasetVersion> linked = new LinkedHashSet<>(model.getOtherDatasets());
        if (model.getTrainedOn() != null) {
            linked.add(model.getTrainedOn());
        }
        if (linked.isEmpty()) {
            // Rien à hériter : l'usage déclaré, s'il y en a un, est simplement conservé
            model.setUsageKind(declared);
            return;
        }
        if (declared == null) {
            throw new BadRequestAlertException("Precisez l'usage accorde a ce modele", ENTITY_NAME, "usagerequired");
        }
        Map<Long, List<LicenseKind>> held = new HashMap<>();
        for (Object[] row : purchaseRepository.findHeldLicenses(owner.getId(), linked.stream().map(DatasetVersion::getId).toList())) {
            held.computeIfAbsent((Long) row[0], id -> new ArrayList<>()).add((LicenseKind) row[1]);
        }
        List<LicenseKind> perVersion = new ArrayList<>();
        for (DatasetVersion version : linked) {
            List<LicenseKind> licenses = held.get(version.getId());
            if (licenses == null || licenses.isEmpty()) {
                throw new BadRequestAlertException("Vous devez avoir achete chaque dataset cite", ENTITY_NAME, "licensenotheld");
            }
            perVersion.add(LicenseInheritance.strongest(licenses).orElseThrow());
        }
        LicenseKind ceiling = LicenseInheritance.ceiling(perVersion).orElseThrow();
        LicenseInheritance.violation(ceiling, declared, model.getLicense()).ifPresent(key -> {
            throw new BadRequestAlertException("Licence incompatible avec les datasets d'entrainement", ENTITY_NAME, key);
        });
        model.setUsageKind(declared);
    }

    /** Seules les versions publiees peuvent etre citees (une brouillon n'est pas visible du public). */
    private DatasetVersion publishedVersion(Long id) {
        if (id == null) {
            throw new BadRequestAlertException("Version de dataset introuvable", ENTITY_NAME, "versionnotfound");
        }
        return versionRepository
            .findById(id)
            .filter(v -> v.getStatus() == VersionStatus.PUBLISHED)
            .orElseThrow(() -> new BadRequestAlertException("Version de dataset introuvable", ENTITY_NAME, "versionnotfound"));
    }

    @Transactional(readOnly = true)
    public List<ModelSummary> mine() {
        UserProfile owner = userProfileService.getOrCreateCurrent();
        return modelRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream().map(catalog::toModelSummary).toList();
    }

    /** Supprime un de mes modeles ; renvoie faux s'il n'existe pas ou n'est pas a moi (meme reponse : rien n'est revele). */
    public boolean deleteMine(Long id) {
        UserProfile owner = userProfileService.getOrCreateCurrent();
        return modelRepository
            .findById(id)
            .filter(m -> m.getOwner() != null && m.getOwner().getId().equals(owner.getId()))
            .map(m -> {
                modelRepository.delete(m);
                return true;
            })
            .orElse(false);
    }

    /**
     * Le lien du modele est affiche sur le site : on n'accepte que https (pas de javascript:, data:, http clair) et
     * un nom d'hote.
     */
    private static String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
            return uri.toString();
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Le lien doit etre une adresse https valide", ENTITY_NAME, "invalidurl");
        }
    }

    /**
     * Empreinte facultative : 64 caracteres hexadecimaux, rangee en minuscules. La plateforme ne telecharge pas le
     * fichier (elle ne stocke que des metadonnees) : l'empreinte est declarative, c'est le lecteur qui la verifie.
     */
    private static String validateSha256(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String hash = value.trim().toLowerCase();
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new BadRequestAlertException(
                "L'empreinte doit etre un SHA-256 (64 caracteres hexadecimaux)",
                ENTITY_NAME,
                "invalidsha256"
            );
        }
        return hash;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
