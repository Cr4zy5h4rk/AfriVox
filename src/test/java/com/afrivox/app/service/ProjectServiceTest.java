package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.Project;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.ProjectRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Projets du hub : etiquettes, liens https, nombre maximal, modele lie reserve a son proprietaire. */
class ProjectServiceTest {

    private ProjectRepository projects;
    private MlModelRepository models;
    private UserProfileService profileService;
    private ProjectService service;
    private UserProfile owner;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        models = mock(MlModelRepository.class);
        profileService = mock(UserProfileService.class);
        service = new ProjectService(projects, models, mock(LanguageRepository.class), profileService);
        owner = new UserProfile().id(1L);
        owner.setDisplayName("awa");
        when(profileService.getOrCreateCurrent()).thenReturn(owner);
        when(projects.save(any(Project.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static ProjectService.ProjectInput input(String repoUrl, List<String> tags, Long modelId) {
        return new ProjectService.ProjectInput(
            "Dictee wolof",
            "Un outil de dictee vocale pour le wolof.",
            repoUrl,
            null,
            null,
            tags,
            modelId
        );
    }

    private static void assertErrorKey(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    // ---- Etiquettes ----------------------------------------------------------------------------------------------

    @Test
    void tagsAreLowercasedSplitAndDeduplicated() {
        assertThat(ProjectService.normalizeTags(List.of("ASR, Wolof", "wolof mobile"))).containsExactly("asr", "wolof", "mobile");
    }

    @Test
    void rejectsAnInvalidTagInsteadOfSilentlyDroppingIt() {
        assertErrorKey(() -> ProjectService.normalizeTags(List.of("bad<tag>")), "invalidtag");
        assertErrorKey(() -> ProjectService.normalizeTags(List.of("x".repeat(25))), "invalidtag");
    }

    @Test
    void refusesMoreThanFiveTags() {
        assertErrorKey(() -> ProjectService.normalizeTags(List.of("a", "b", "c", "d", "e", "f")), "toomanytags");
    }

    @Test
    void noTagsIsFine() {
        assertThat(ProjectService.normalizeTags(null)).isEmpty();
        assertThat(ProjectService.normalizeTags(List.of(" ", ","))).isEmpty();
    }

    // ---- Creation ------------------------------------------------------------------------------------------------

    @Test
    void createsAProjectWithNormalizedTagsAndAnHttpsLink() {
        ProjectService.ProjectView view = service.create(input("https://example.org/repo", List.of("ASR", "Wolof"), null));

        assertThat(view.tags()).containsExactly("asr", "wolof");
        assertThat(view.repoUrl()).isEqualTo("https://example.org/repo");
        assertThat(view.ownerName()).isEqualTo("awa");
        assertThat(view.canDelete()).isFalse(); // pas de session dans ce test : le visiteur n'est pas reconnu comme l'auteur
    }

    @Test
    void refusesLinksThatAreNotPlainHttps() {
        for (String url : List.of(
            "javascript:alert(1)",
            "http://example.org",
            "data:text/html,x",
            "ftp://example.org",
            "https://user:pw@example.org"
        )) {
            assertErrorKey(() -> service.create(input(url, List.of(), null)), "invalidurl");
        }
        verify(projects, never()).save(any());
    }

    @Test
    void validatesTitleAndSummaryLength() {
        assertErrorKey(
            () -> service.create(new ProjectService.ProjectInput("ab", "Un resume suffisamment long.", null, null, null, null, null)),
            "invalidtitle"
        );
        assertErrorKey(
            () -> service.create(new ProjectService.ProjectInput("Titre valide", "court", null, null, null, null, null)),
            "invalidsummary"
        );
    }

    @Test
    void refusesAProjectBeyondTheMaximumPerOwner() {
        when(projects.countByOwnerId(1L)).thenReturn((long) ProjectService.MAX_PROJECTS_PER_OWNER);

        assertErrorKey(() -> service.create(input(null, List.of(), null)), "toomanyprojects");
    }

    @Test
    void canOnlyLinkOneOfYourOwnModels() {
        UserProfile other = new UserProfile().id(2L);
        MlModel foreign = new MlModel();
        foreign.setOwner(other);
        when(models.findById(7L)).thenReturn(Optional.of(foreign));

        assertErrorKey(() -> service.create(input(null, List.of(), 7L)), "modelnotfound");

        MlModel mine = new MlModel();
        mine.setOwner(owner);
        when(models.findById(8L)).thenReturn(Optional.of(mine));
        assertThat(service.create(input(null, List.of(), 8L))).isNotNull();
    }

    // ---- Modification et suppression ------------------------------------------------------------------------------

    @Test
    void cannotUpdateSomeoneElsesProject() {
        Project theirs = new Project();
        theirs.setOwner(new UserProfile().id(2L));
        when(projects.findWithOwner(5L)).thenReturn(Optional.of(theirs));

        assertThat(service.update(5L, input(null, List.of(), null))).isEmpty();
        verify(projects, never()).save(any());
    }

    @Test
    void cannotDeleteSomeoneElsesProjectWithoutBeingAdmin() {
        Project theirs = new Project();
        theirs.setOwner(new UserProfile().id(2L));
        when(projects.findWithOwner(5L)).thenReturn(Optional.of(theirs));

        assertThat(service.delete(5L)).isFalse();
        verify(projects, never()).delete(any(Project.class));
    }

    @Test
    void deletesYourOwnProject() {
        Project mine = new Project();
        mine.setOwner(owner);
        when(projects.findWithOwner(5L)).thenReturn(Optional.of(mine));

        assertThat(service.delete(5L)).isTrue();
        verify(projects).delete(mine);
    }
}
