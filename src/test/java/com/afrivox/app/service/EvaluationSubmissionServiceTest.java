package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.Evaluation;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.TestReference;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.MlTask;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.EvaluationRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.TestReferenceRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Classement par predictions : lecture du CSV (c'est une entree utilisateur) et garde-fous de l'envoi. Le calcul des
 * taux lui-meme est couvert par {@link ErrorRateTest}.
 */
class EvaluationSubmissionServiceTest {

    private static final long VERSION_ID = 50L;
    private static final long MODEL_ID = 10L;
    private static final long ME_ID = 1L;

    private MlModelRepository models;
    private DatasetVersionRepository versions;
    private TestReferenceRepository references;
    private EvaluationRepository evaluations;
    private UserProfileService profileService;
    private EvaluationSubmissionService service;

    private UserProfile me;
    private Language wolof;
    private MlModel model;
    private DatasetVersion version;

    @BeforeEach
    void setUp() {
        models = mock(MlModelRepository.class);
        versions = mock(DatasetVersionRepository.class);
        references = mock(TestReferenceRepository.class);
        evaluations = mock(EvaluationRepository.class);
        profileService = mock(UserProfileService.class);
        service = new EvaluationSubmissionService(models, versions, references, evaluations, profileService);

        me = new UserProfile().id(ME_ID);
        when(profileService.getOrCreateCurrent()).thenReturn(me);

        wolof = new Language().id(1L);
        wolof.setCode("wo");
        model = new MlModel().id(MODEL_ID);
        model.setTask(MlTask.ASR);
        model.setOwner(me);
        model.setLanguage(wolof);
        when(models.findById(MODEL_ID)).thenReturn(Optional.of(model));

        Dataset dataset = new Dataset();
        dataset.setLanguage(wolof);
        version = new DatasetVersion().id(VERSION_ID);
        version.setStatus(VersionStatus.PUBLISHED);
        version.setDataset(dataset);
        when(versions.findById(VERSION_ID)).thenReturn(Optional.of(version));

        when(references.findAllByVersionIdOrderByRecordingId(VERSION_ID)).thenReturn(
            List.of(new TestReference(VERSION_ID, 1L, "nanga def"), new TestReference(VERSION_ID, 2L, "mangi fi rekk"))
        );
        when(evaluations.findFirstByModelIdAndTestSetIdOrderBySubmittedAtDesc(MODEL_ID, VERSION_ID)).thenReturn(Optional.empty());
        when(evaluations.save(any(Evaluation.class))).thenAnswer(call -> {
            Evaluation e = call.getArgument(0);
            e.setId(900L);
            return e;
        });
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    private static byte[] csv(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertErrorKey(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    private static void assertParseError(byte[] content, String key) {
        assertErrorKey(() -> EvaluationSubmissionService.parse(content, 100), key);
    }

    // ---- Lecture du CSV ------------------------------------------------------------------------------------------

    @Test
    void parsesACommaSeparatedFile() {
        Map<Long, String> rows = EvaluationSubmissionService.parse(csv("id,text\n1,nanga def\n2,mangi fi rekk\n"), 10);

        assertThat(rows).containsEntry(1L, "nanga def").containsEntry(2L, "mangi fi rekk").hasSize(2);
    }

    @Test
    void parsesATabSeparatedFileWithCrLfAndBom() {
        Map<Long, String> rows = EvaluationSubmissionService.parse(csv("﻿id\ttext\r\n1\tnanga def, nit\r\n2\tjam\r\n"), 10);

        assertThat(rows).containsEntry(1L, "nanga def, nit").containsEntry(2L, "jam");
    }

    @Test
    void parsesQuotedFieldsWithCommasQuotesAndLineBreaks() {
        Map<Long, String> rows = EvaluationSubmissionService.parse(csv("id,text\n1,\"a, b \"\"c\"\"\"\n2,\"ligne un\nligne deux\"\n"), 10);

        assertThat(rows).containsEntry(1L, "a, b \"c\"").containsEntry(2L, "ligne un\nligne deux");
    }

    @Test
    void headerIsCaseInsensitiveAndBlankLinesAreSkipped() {
        Map<Long, String> rows = EvaluationSubmissionService.parse(csv("ID,Text\n\n1,a\n\n"), 10);

        assertThat(rows).containsOnlyKeys(1L);
    }

    @Test
    void emptyTextIsAllowedItCountsAsAWrongPrediction() {
        assertThat(EvaluationSubmissionService.parse(csv("id,text\n1,\n"), 10)).containsEntry(1L, "");
    }

    @Test
    void rejectsEmptyOrBlankFiles() {
        assertParseError(new byte[0], "csvinvalid");
        assertParseError(null, "csvinvalid");
        assertParseError(csv("\n\n"), "csvinvalid");
    }

    @Test
    void rejectsFilesOverTheSizeLimit() {
        assertParseError(new byte[EvaluationSubmissionService.MAX_BYTES + 1], "csvtoolarge");
    }

    @Test
    void rejectsNonUtf8Content() {
        assertParseError(new byte[] { 'i', 'd', ',', 't', 'e', 'x', 't', '\n', '1', ',', (byte) 0xFF, (byte) 0xFE, '\n' }, "csvinvalid");
    }

    @Test
    void rejectsAWrongHeader() {
        assertParseError(csv("identifiant,texte\n1,a\n"), "csvinvalid");
        assertParseError(csv("id,text,extra\n1,a,b\n"), "csvinvalid");
    }

    @Test
    void rejectsRowsThatDoNotHaveTwoColumns() {
        assertParseError(csv("id,text\n1,a,b\n"), "csvinvalid");
        assertParseError(csv("id,text\n1\n"), "csvinvalid");
    }

    @Test
    void rejectsNonNumericIdentifiers() {
        assertParseError(csv("id,text\nabc,a\n"), "csvinvalid");
    }

    @Test
    void rejectsUnclosedQuotes() {
        assertParseError(csv("id,text\n1,\"jamais ferme\n"), "csvinvalid");
    }

    @Test
    void rejectsDuplicateIdentifiers() {
        assertParseError(csv("id,text\n1,a\n1,b\n"), "idduplicate");
    }

    @Test
    void rejectsSentencesOverTheWordLimit() {
        String longSentence = "mot ".repeat(EvaluationSubmissionService.MAX_WORDS_PER_LINE + 1);

        assertParseError(csv("id,text\n1," + longSentence + "\n"), "texttoolong");
    }

    @Test
    void acceptsASentenceAtTheWordLimit() {
        String sentence = "mot ".repeat(EvaluationSubmissionService.MAX_WORDS_PER_LINE).trim();

        assertThatCode(() -> EvaluationSubmissionService.parse(csv("id,text\n1," + sentence + "\n"), 10)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreRowsThanTheTestSetHas() {
        assertErrorKey(() -> EvaluationSubmissionService.parse(csv("id,text\n1,a\n2,b\n3,c\n"), 2), "idunknown");
    }

    // ---- Envoi ---------------------------------------------------------------------------------------------------

    @Test
    void perfectPredictionsScoreZero() {
        EvaluationSubmissionService.Submission result = service.submit(
            MODEL_ID,
            VERSION_ID,
            csv("id,text\n1,nanga def\n2,mangi fi rekk\n")
        );

        assertThat(result.wer()).isZero();
        assertThat(result.cer()).isZero();
        assertThat(result.sentences()).isEqualTo(2);
        assertThat(result.evaluationId()).isEqualTo(900L);
    }

    @Test
    void scoreIsComputedOverTheWholeTestSetAndRoundedToTwoDecimals() {
        // 1 mot faux sur 5 = 20 % ; « def » -> « bi » = 3 editions sur 22 caracteres = 13,64 %.
        service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,nanga bi\n2,mangi fi rekk\n"));

        ArgumentCaptor<Evaluation> captor = ArgumentCaptor.forClass(Evaluation.class);
        verify(evaluations).save(captor.capture());
        assertThat(captor.getValue().getWer()).isEqualTo(20.0f);
        assertThat(captor.getValue().getCer()).isEqualTo(13.64f);
        assertThat(captor.getValue().getModel()).isSameAs(model);
        assertThat(captor.getValue().getTestSet()).isSameAs(version);
    }

    @Test
    void anotherDevelopersModelLooksNonexistent() {
        model.setOwner(new UserProfile().id(999L));

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "modelnotfound");
        verify(evaluations, never()).save(any());
    }

    @Test
    void adminCanSubmitForAnyModel() {
        model.setOwner(new UserProfile().id(999L));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        assertThatCode(() ->
            service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,nanga def\n2,mangi fi rekk\n"))
        ).doesNotThrowAnyException();
    }

    @Test
    void onlyAsrModelsAreEvaluated() {
        model.setTask(MlTask.TTS);

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "tasknotasr");
    }

    @Test
    void unpublishedVersionIsNotATestSet() {
        version.setStatus(VersionStatus.WITHDRAWN);

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "testsetnotfound");
    }

    @Test
    void versionWithoutReferencesHasNoTestSet() {
        when(references.findAllByVersionIdOrderByRecordingId(VERSION_ID)).thenReturn(List.of());

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n")), "testsetnotfound");
    }

    @Test
    void modelLanguageMustMatchTheTestSet() {
        Language pulaar = new Language().id(2L);
        pulaar.setCode("ff");
        model.setLanguage(pulaar);

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "languagemismatch");
    }

    @Test
    void secondSubmissionWithinTenMinutesIsRefused() {
        Evaluation last = new Evaluation();
        last.setSubmittedAt(Instant.now().minus(Duration.ofMinutes(1)));
        when(evaluations.findFirstByModelIdAndTestSetIdOrderBySubmittedAtDesc(MODEL_ID, VERSION_ID)).thenReturn(Optional.of(last));

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "toosoon");
    }

    @Test
    void submissionIsAllowedAgainAfterTheDelay() {
        Evaluation last = new Evaluation();
        last.setSubmittedAt(Instant.now().minus(Duration.ofMinutes(11)));
        when(evaluations.findFirstByModelIdAndTestSetIdOrderBySubmittedAtDesc(MODEL_ID, VERSION_ID)).thenReturn(Optional.of(last));

        assertThatCode(() ->
            service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,nanga def\n2,mangi fi rekk\n"))
        ).doesNotThrowAnyException();
    }

    @Test
    void everySentenceMustBeAnswered() {
        // Un sous-ensemble choisi (les phrases faciles) fausserait le classement.
        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,nanga def\n")), "idsmissing");
        verify(evaluations, never()).save(any());
    }

    @Test
    void identifiersOutsideTheTestSetAreRefused() {
        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n99,b\n")), "idunknown");
    }

    @Test
    void testSetWithoutReferenceTextIsRefused() {
        when(references.findAllByVersionIdOrderByRecordingId(VERSION_ID)).thenReturn(
            List.of(new TestReference(VERSION_ID, 1L, ""), new TestReference(VERSION_ID, 2L, "..."))
        );

        assertErrorKey(() -> service.submit(MODEL_ID, VERSION_ID, csv("id,text\n1,a\n2,b\n")), "testsetnotfound");
    }

    // ---- Modele de CSV -------------------------------------------------------------------------------------------

    @Test
    void templateListsEveryIdentifierWithAnEmptyText() {
        assertThat(service.template(VERSION_ID)).isEqualTo("id,text\n1,\n2,\n");
    }

    @Test
    void templateOfAnUnknownVersionFails() {
        when(references.findAllByVersionIdOrderByRecordingId(404L)).thenReturn(List.of());

        assertErrorKey(() -> service.template(404L), "testsetnotfound");
    }
}
