package com.afrivox.app.service;

import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.ProjectRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.service.ProjectService.ProjectView;
import com.afrivox.app.service.PublicCatalogService.ModelSummary;
import com.afrivox.app.service.PublicCatalogService.PageView;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annuaire et profils publics des developpeurs. Principe : rien n'est public sans choix explicite. Un profil non
 * public n'apparait ni dans l'annuaire ni par son numero (meme reponse « introuvable »). Un profil public montre le
 * nom affiche, la presentation, un lien choisi, les modeles et les projets ; jamais l'e-mail, le pays ni les
 * informations de contributeur. « Ouvert aux opportunites » n'a d'effet que sur un profil public.
 */
@Service
@Transactional
public class DeveloperProfileService {

    private static final String ENTITY_NAME = "developerProfile";

    static final int MAX_PAGE_SIZE = 48;
    static final int MAX_BIO = 300;

    public record DeveloperSummary(Long id, String name, String bio, boolean openToWork, long models, long projects) {}

    public record DeveloperProfile(
        Long id,
        String name,
        String bio,
        String profileUrl,
        boolean openToWork,
        List<ModelSummary> models,
        List<ProjectView> projects
    ) {}

    /** Reglages du developpeur connecte. */
    public record Settings(Long id, String displayName, String bio, String profileUrl, boolean publicProfile, boolean openToWork) {}

    private final UserProfileRepository profileRepository;
    private final MlModelRepository modelRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final PublicCatalogService catalog;
    private final UserProfileService userProfileService;

    public DeveloperProfileService(
        UserProfileRepository profileRepository,
        MlModelRepository modelRepository,
        ProjectRepository projectRepository,
        ProjectService projectService,
        PublicCatalogService catalog,
        UserProfileService userProfileService
    ) {
        this.profileRepository = profileRepository;
        this.modelRepository = modelRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.catalog = catalog;
        this.userProfileService = userProfileService;
    }

    @Transactional(readOnly = true)
    public PageView<DeveloperSummary> search(String q, boolean openToWork, int page, int size) {
        String text = q == null || q.isBlank() ? null : "%" + escape(q.strip().toLowerCase(Locale.ROOT)) + "%";
        Page<UserProfile> result = profileRepository.searchPublic(
            text,
            openToWork,
            PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)), Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        return new PageView<>(
            result
                .getContent()
                .stream()
                .map(p ->
                    new DeveloperSummary(
                        p.getId(),
                        p.getDisplayName(),
                        p.getBio(),
                        p.isOpenToWork(),
                        modelRepository.countByOwnerId(p.getId()),
                        projectRepository.countByOwnerId(p.getId())
                    )
                )
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    /** Profil public ; vide si le profil n'existe pas ou n'est pas public (les deux cas se ressemblent a dessein). */
    @Transactional(readOnly = true)
    public Optional<DeveloperProfile> get(Long id) {
        return profileRepository
            .findById(id)
            .filter(UserProfile::isPublicProfile)
            .map(p ->
                new DeveloperProfile(
                    p.getId(),
                    p.getDisplayName(),
                    p.getBio(),
                    p.getProfileUrl(),
                    p.isOpenToWork(),
                    modelRepository.findAllByOwnerIdOrderByCreatedAtDesc(p.getId()).stream().map(catalog::toModelSummary).toList(),
                    projectService.viewsOf(projectRepository.findByOwner(p.getId()))
                )
            );
    }

    /** Pas de {@code readOnly} : le profil peut etre cree au premier usage. */
    public Settings settings() {
        return toSettings(userProfileService.getOrCreateCurrent());
    }

    public Settings update(Settings input) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        String name = input.displayName() == null ? "" : input.displayName().strip();
        if (name.length() < 2 || name.length() > 80) {
            throw new BadRequestAlertException("Le nom affiche doit faire entre 2 et 80 caracteres", ENTITY_NAME, "invalidname");
        }
        String bio = input.bio() == null || input.bio().isBlank() ? null : input.bio().strip();
        if (bio != null && bio.length() > MAX_BIO) {
            throw new BadRequestAlertException("La presentation est limitee a 300 caracteres", ENTITY_NAME, "invalidbio");
        }
        me.setDisplayName(name);
        me.setBio(bio);
        me.setProfileUrl(SafeUrl.https(input.profileUrl(), ENTITY_NAME, "invalidurl"));
        me.setPublicProfile(input.publicProfile());
        // Sans profil public, personne ne peut voir ni contacter : on ne garde pas un « ouvert » invisible.
        me.setOpenToWork(input.publicProfile() && input.openToWork());
        return toSettings(profileRepository.save(me));
    }

    private static Settings toSettings(UserProfile p) {
        return new Settings(p.getId(), p.getDisplayName(), p.getBio(), p.getProfileUrl(), p.isPublicProfile(), p.isOpenToWork());
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
