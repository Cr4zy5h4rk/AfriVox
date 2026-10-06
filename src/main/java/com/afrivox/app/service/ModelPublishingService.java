package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
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
        String languageCode,
        Long trainedOnVersionId
    ) {}

    private final MlModelRepository modelRepository;
    private final LanguageRepository languageRepository;
    private final DatasetVersionRepository versionRepository;
    private final UserProfileService userProfileService;
    private final PublicCatalogService catalog;

    public ModelPublishingService(
        MlModelRepository modelRepository,
        LanguageRepository languageRepository,
        DatasetVersionRepository versionRepository,
        UserProfileService userProfileService,
        PublicCatalogService catalog
    ) {
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
            DatasetVersion version = versionRepository
                .findById(input.trainedOnVersionId())
                .filter(v -> v.getStatus() == VersionStatus.PUBLISHED)
                .orElseThrow(() -> new BadRequestAlertException("Version de dataset introuvable", ENTITY_NAME, "versionnotfound"));
            model.setTrainedOn(version);
        }
        return catalog.toModelSummary(modelRepository.save(model));
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
