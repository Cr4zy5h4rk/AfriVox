package com.afrivox.app.service;

import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.Project;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.ProjectRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.service.PublicCatalogService.PageView;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projets du hub des developpeurs. La liste et la fiche sont publiques ; publier, modifier et supprimer sont reserves
 * aux developpeurs (et a l'admin) et ne concernent que leurs propres projets (l'admin peut en supprimer n'importe
 * lequel : moderation). Un projet n'heberge rien : seulement des liens https et du texte, affiches comme du texte.
 */
@Service
@Transactional
public class ProjectService {

    private static final String ENTITY_NAME = "project";

    static final int MAX_PAGE_SIZE = 48;
    /** Au plus {@value} projets par auteur (contre le remplissage du hub). */
    static final int MAX_PROJECTS_PER_OWNER = 20;
    static final int MAX_TAGS = 5;
    private static final Pattern TAG = Pattern.compile("[a-z0-9][a-z0-9+#._-]{0,23}");

    /** Donnees saisies pour creer ou modifier un projet. */
    public record ProjectInput(
        String title,
        String summary,
        String repoUrl,
        String demoUrl,
        String languageCode,
        List<String> tags,
        Long modelId
    ) {}

    public record ProjectView(
        Long id,
        String title,
        String summary,
        String repoUrl,
        String demoUrl,
        String languageCode,
        String languageName,
        List<String> tags,
        Long modelId,
        String modelName,
        Long ownerId,
        String ownerName,
        /** Le profil de l'auteur est public : l'interface peut afficher le lien vers sa page. */
        boolean ownerPublic,
        Instant createdAt,
        Instant updatedAt,
        /** Le visiteur connecte est l'auteur. */
        boolean mine,
        /** Le visiteur peut supprimer le projet (son auteur ou l'admin). */
        boolean canDelete
    ) {}

    private final ProjectRepository projectRepository;
    private final MlModelRepository modelRepository;
    private final LanguageRepository languageRepository;
    private final UserProfileService userProfileService;

    public ProjectService(
        ProjectRepository projectRepository,
        MlModelRepository modelRepository,
        LanguageRepository languageRepository,
        UserProfileService userProfileService
    ) {
        this.projectRepository = projectRepository;
        this.modelRepository = modelRepository;
        this.languageRepository = languageRepository;
        this.userProfileService = userProfileService;
    }

    @Transactional(readOnly = true)
    public PageView<ProjectView> search(String q, String language, String tag, int page, int size) {
        String text = q == null || q.isBlank() ? null : like(q.strip().toLowerCase(Locale.ROOT));
        String lang = language == null || language.isBlank() ? null : language.strip().toLowerCase(Locale.ROOT);
        String tagPattern = tag == null || tag.isBlank() ? null : "%," + escape(tag.strip().toLowerCase(Locale.ROOT)) + ",%";
        Page<Project> result = projectRepository.search(
            text,
            lang,
            tagPattern,
            PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)), Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );
        Viewer viewer = viewer();
        Map<String, String> names = languageNames();
        return new PageView<>(
            result
                .getContent()
                .stream()
                .map(p -> view(p, viewer, names))
                .toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Optional<ProjectView> get(Long id) {
        Viewer viewer = viewer();
        Map<String, String> names = languageNames();
        return projectRepository.findWithOwner(id).map(p -> view(p, viewer, names));
    }

    /** Projets du developpeur connecte. Pas de {@code readOnly} : son profil peut etre cree au premier usage. */
    public List<ProjectView> mine() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Viewer viewer = viewer();
        Map<String, String> names = languageNames();
        return projectRepository
            .findByOwner(me.getId())
            .stream()
            .map(p -> view(p, viewer, names))
            .toList();
    }

    public ProjectView create(ProjectInput input) {
        UserProfile owner = userProfileService.getOrCreateCurrent();
        if (projectRepository.countByOwnerId(owner.getId()) >= MAX_PROJECTS_PER_OWNER) {
            throw new BadRequestAlertException("Trop de projets", ENTITY_NAME, "toomanyprojects");
        }
        Project project = new Project();
        project.setOwner(owner);
        project.setCreatedAt(Instant.now());
        apply(project, input, owner);
        return view(projectRepository.save(project), viewer(), languageNames());
    }

    /** Modifie un de ses projets ; {@code empty} s'il n'existe pas ou n'est pas a l'utilisateur. */
    public Optional<ProjectView> update(Long id, ProjectInput input) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        return projectRepository
            .findWithOwner(id)
            .filter(p -> p.getOwner().getId().equals(me.getId()))
            .map(p -> {
                apply(p, input, me);
                return view(projectRepository.save(p), viewer(), languageNames());
            });
    }

    /** Supprime un projet (ses commentaires partent avec lui) ; faux s'il n'existe pas ou n'est pas a l'utilisateur. */
    public boolean delete(Long id) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        boolean admin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        return projectRepository
            .findWithOwner(id)
            .filter(p -> admin || p.getOwner().getId().equals(me.getId()))
            .map(p -> {
                projectRepository.delete(p);
                return true;
            })
            .orElse(false);
    }

    private void apply(Project project, ProjectInput input, UserProfile owner) {
        String title = input.title() == null ? "" : input.title().strip();
        String summary = input.summary() == null ? "" : input.summary().strip();
        if (title.length() < 3 || title.length() > 120) {
            throw new BadRequestAlertException("Le titre doit faire entre 3 et 120 caracteres", ENTITY_NAME, "invalidtitle");
        }
        if (summary.length() < 10 || summary.length() > 1000) {
            throw new BadRequestAlertException("Le resume doit faire entre 10 et 1000 caracteres", ENTITY_NAME, "invalidsummary");
        }
        project.setTitle(title);
        project.setSummary(summary);
        project.setRepoUrl(SafeUrl.https(input.repoUrl(), ENTITY_NAME, "invalidurl"));
        project.setDemoUrl(SafeUrl.https(input.demoUrl(), ENTITY_NAME, "invalidurl"));

        if (input.languageCode() == null || input.languageCode().isBlank()) {
            project.setLanguageCode(null);
        } else {
            Language language = languageRepository
                .findOneByCode(input.languageCode().strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestAlertException("Langue inconnue", ENTITY_NAME, "languagenotfound"));
            project.setLanguageCode(language.getCode());
        }

        Set<String> tags = normalizeTags(input.tags());
        project.setTags(tags.isEmpty() ? null : String.join(",", tags));

        if (input.modelId() == null) {
            project.setModel(null);
        } else {
            // On ne lie qu'un de ses propres modeles : sinon on afficherait le projet sur la fiche d'un autre.
            MlModel model = modelRepository
                .findById(input.modelId())
                .filter(m -> m.getOwner() != null && m.getOwner().getId().equals(owner.getId()))
                .orElseThrow(() -> new BadRequestAlertException("Modele introuvable", ENTITY_NAME, "modelnotfound"));
            project.setModel(model);
        }
        project.setUpdatedAt(Instant.now());
    }

    /** Minuscules, motif restreint, sans doublon, {@value #MAX_TAGS} au plus : refuse plutot que de tronquer en silence. */
    static Set<String> normalizeTags(List<String> raw) {
        Set<String> tags = new LinkedHashSet<>();
        if (raw == null) {
            return tags;
        }
        for (String item : raw) {
            for (String part : item == null ? new String[0] : item.split("[,\\s]+")) {
                String tag = part.strip().toLowerCase(Locale.ROOT);
                if (tag.isEmpty()) {
                    continue;
                }
                if (!TAG.matcher(tag).matches()) {
                    throw new BadRequestAlertException("Etiquette invalide", ENTITY_NAME, "invalidtag");
                }
                tags.add(tag);
            }
        }
        if (tags.size() > MAX_TAGS) {
            throw new BadRequestAlertException("Trop d'etiquettes", ENTITY_NAME, "toomanytags");
        }
        return tags;
    }

    /** Vues d'une liste de projets deja chargee (profil public d'un developpeur). */
    List<ProjectView> viewsOf(List<Project> projects) {
        Viewer viewer = viewer();
        Map<String, String> names = languageNames();
        return projects
            .stream()
            .map(p -> view(p, viewer, names))
            .toList();
    }

    /** Visiteur courant : son login (pour « mon projet ») et son role admin. */
    private record Viewer(String login, boolean admin) {}

    private static Viewer viewer() {
        return new Viewer(
            SecurityUtils.getCurrentUserLogin().orElse(null),
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)
        );
    }

    private ProjectView view(Project p, Viewer viewer, Map<String, String> languageNames) {
        UserProfile owner = p.getOwner();
        boolean mine = viewer.login() != null && owner.getUser() != null && viewer.login().equals(owner.getUser().getLogin());
        MlModel model = p.getModel();
        String languageName = p.getLanguageCode() == null ? null : languageNames.get(p.getLanguageCode());
        return new ProjectView(
            p.getId(),
            p.getTitle(),
            p.getSummary(),
            p.getRepoUrl(),
            p.getDemoUrl(),
            p.getLanguageCode(),
            languageName,
            p.getTags() == null || p.getTags().isBlank() ? List.of() : Arrays.asList(p.getTags().split(",")),
            model == null ? null : model.getId(),
            model == null ? null : model.getName(),
            owner.getId(),
            owner.getDisplayName(),
            owner.isPublicProfile(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            mine,
            mine || viewer.admin()
        );
    }

    /** Noms des langues par code, lus une fois par requete (pas une lecture par projet). */
    private Map<String, String> languageNames() {
        Map<String, String> names = new HashMap<>();
        languageRepository.findAllByOrderByNameAsc().forEach(l -> names.put(l.getCode(), l.getName()));
        return names;
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String like(String text) {
        return "%" + escape(text) + "%";
    }
}
