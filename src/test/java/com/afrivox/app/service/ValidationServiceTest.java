package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.LanguageQualification;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.RecordingReview;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.domain.enumeration.Verdict;
import com.afrivox.app.repository.LanguageQualificationRepository;
import com.afrivox.app.repository.LanguageRepository;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.repository.RecordingReviewRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regle de consensus (2 avis concordants sur 3 au plus), taches pieges et revocation d'un validateur peu fiable.
 * Les depots sont simules : on verifie la decision, pas la persistance.
 */
class ValidationServiceTest {

    private static final long RECORDING_ID = 100L;
    private static final long AUTHOR_ID = 10L;
    private static final long ME_ID = 20L;
    private static final long LANGUAGE_ID = 1L;

    private RecordingRepository recordings;
    private RecordingReviewRepository reviews;
    private LanguageQualificationRepository qualifications;
    private LanguageRepository languages;
    private LanguageActivationService activation;
    private UserProfileService profileService;
    private ContributorTrialService trial;
    private CreditService credits;
    private ValidationService service;

    private UserProfile author;
    private UserProfile me;
    private Recording recording;
    private LanguageQualification myQualification;
    /** Avis deja en base ; {@code save} y ajoute le nouvel avis pour que le consensus le voie. */
    private List<RecordingReview> stored;

    @BeforeEach
    void setUp() {
        recordings = mock(RecordingRepository.class);
        reviews = mock(RecordingReviewRepository.class);
        qualifications = mock(LanguageQualificationRepository.class);
        languages = mock(LanguageRepository.class);
        activation = mock(LanguageActivationService.class);
        profileService = mock(UserProfileService.class);
        trial = mock(ContributorTrialService.class);
        credits = mock(CreditService.class);
        service = new ValidationService(
            new ApplicationProperties(),
            recordings,
            reviews,
            qualifications,
            languages,
            activation,
            profileService,
            trial,
            credits
        );

        Language language = new Language().id(LANGUAGE_ID);
        Campaign campaign = new Campaign();
        campaign.setLanguage(language);
        Prompt prompt = new Prompt();
        prompt.setCampaign(campaign);
        prompt.setContent("Nanga def ?");

        author = new UserProfile().id(AUTHOR_ID);
        me = new UserProfile().id(ME_ID);
        recording = new Recording().id(RECORDING_ID).status(RecordingStatus.SUBMITTED);
        recording.setContributor(author);
        recording.setPrompt(prompt);

        myQualification = new LanguageQualification();
        myQualification.setProfile(me);
        myQualification.setLanguage(language);
        myQualification.setStatus(QualificationStatus.QUALIFIED);

        stored = new ArrayList<>();
        when(profileService.getOrCreateCurrent()).thenReturn(me);
        when(recordings.findById(RECORDING_ID)).thenReturn(Optional.of(recording));
        when(qualifications.findOneByProfileIdAndLanguageId(ME_ID, LANGUAGE_ID)).thenReturn(Optional.of(myQualification));
        when(reviews.findAllByRecordingId(RECORDING_ID)).thenAnswer(call -> new ArrayList<>(stored));
        when(reviews.save(any(RecordingReview.class))).thenAnswer(call -> {
            RecordingReview saved = call.getArgument(0);
            stored.add(saved);
            return saved;
        });
    }

    // ---- Aides ---------------------------------------------------------------------------------------------------

    private static RecordingReview earlier(long reviewerId, Verdict verdict, boolean gold) {
        return new RecordingReview().reviewer(new UserProfile().id(reviewerId)).verdict(verdict).gold(gold);
    }

    private void submit(Verdict verdict) {
        service.submit(new ValidationService.ReviewInput(RECORDING_ID, verdict, true, true, true, null));
    }

    private void stubTrapStats(long total, long correct) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { total, correct });
        when(reviews.trapStats(ME_ID, LANGUAGE_ID)).thenReturn(rows);
    }

    private static void assertStatus(Runnable action, HttpStatus expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, e ->
            assertThat(e.getStatusCode().value()).isEqualTo(expected.value())
        );
    }

    // ---- Controles d'acces ---------------------------------------------------------------------------------------

    @Test
    void authorCannotReviewTheirOwnRecording() {
        recording.setContributor(me);

        assertThatThrownBy(() -> submit(Verdict.ACCEPT)).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo("ownrecording")
        );
        verify(reviews, never()).save(any());
    }

    @Test
    void unqualifiedReviewerIsForbidden() {
        myQualification.setStatus(QualificationStatus.PENDING);

        assertStatus(() -> submit(Verdict.ACCEPT), HttpStatus.FORBIDDEN);
        verify(reviews, never()).save(any());
    }

    @Test
    void revokedReviewerIsForbidden() {
        myQualification.setStatus(QualificationStatus.REVOKED);

        assertStatus(() -> submit(Verdict.ACCEPT), HttpStatus.FORBIDDEN);
    }

    @Test
    void sameReviewerCannotReviewTwice() {
        when(reviews.existsByRecordingIdAndReviewerId(RECORDING_ID, ME_ID)).thenReturn(true);

        assertStatus(() -> submit(Verdict.ACCEPT), HttpStatus.CONFLICT);
    }

    // ---- Consensus -----------------------------------------------------------------------------------------------

    @Test
    void oneReviewDoesNotSettleAnything() {
        submit(Verdict.ACCEPT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.SUBMITTED);
        verify(recordings, never()).save(any());
        verify(credits, never()).awardRecordingValidated(any());
    }

    @Test
    void twoAcceptsValidateTheRecordingAndPayAuthorAndAgreeingReviewers() {
        RecordingReview first = earlier(30L, Verdict.ACCEPT, false);
        stored.add(first);

        submit(Verdict.ACCEPT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.VALIDATED);
        verify(recordings).save(recording);
        verify(credits).awardRecordingValidated(recording);
        verify(credits).awardReviewAgreed(recording, first.getReviewer());
        verify(credits).awardReviewAgreed(recording, me);
        verify(trial).evaluate(author);
    }

    @Test
    void twoRejectsRejectTheRecordingAndPayNoAuthorReward() {
        RecordingReview first = earlier(30L, Verdict.REJECT, false);
        stored.add(first);

        submit(Verdict.REJECT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.REJECTED);
        verify(credits, never()).awardRecordingValidated(any());
        // Les validateurs qui ont vu juste sont payes, y compris pour un rejet.
        verify(credits).awardReviewAgreed(recording, first.getReviewer());
        verify(credits).awardReviewAgreed(recording, me);
        verify(trial).evaluate(author);
    }

    @Test
    void dissentingReviewerIsNotPaid() {
        RecordingReview accept = earlier(30L, Verdict.ACCEPT, false);
        RecordingReview reject = earlier(31L, Verdict.REJECT, false);
        stored.add(accept);
        stored.add(reject);

        submit(Verdict.ACCEPT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.VALIDATED);
        verify(credits).awardReviewAgreed(recording, accept.getReviewer());
        verify(credits).awardReviewAgreed(recording, me);
        verify(credits, never()).awardReviewAgreed(recording, reject.getReviewer());
    }

    @Test
    void splitVoteBetweenTwoReviewersLeavesTheRecordingOpen() {
        stored.add(earlier(30L, Verdict.ACCEPT, false));

        submit(Verdict.REJECT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.SUBMITTED);
        verify(recordings, never()).save(any());
    }

    @Test
    void trapReviewsDoNotCountTowardsConsensus() {
        // Deux avis « piege » ACCEPT + le mien : un seul avis reel, rien n'est tranche.
        stored.add(earlier(30L, Verdict.ACCEPT, true));
        stored.add(earlier(31L, Verdict.ACCEPT, true));

        submit(Verdict.ACCEPT);

        assertThat(recording.getStatus()).isEqualTo(RecordingStatus.SUBMITTED);
    }

    @Test
    void recordingWithEnoughReviewsRefusesAnotherOne() {
        stored.add(earlier(30L, Verdict.ACCEPT, false));
        stored.add(earlier(31L, Verdict.REJECT, false));
        stored.add(earlier(32L, Verdict.REJECT, false));

        assertStatus(() -> submit(Verdict.ACCEPT), HttpStatus.CONFLICT);
    }

    // ---- Taches pieges -------------------------------------------------------------------------------------------

    private void settledUnanimously(RecordingStatus status, Verdict verdict) {
        recording.setStatus(status);
        stored.add(earlier(30L, verdict, false));
        stored.add(earlier(31L, verdict, false));
    }

    @Test
    void correctTrapIsPaidButDoesNotChangeTheRecording() {
        settledUnanimously(RecordingStatus.VALIDATED, Verdict.ACCEPT);
        stubTrapStats(1, 1);

        submit(Verdict.ACCEPT);

        verify(credits).awardTrapCorrect(recording, me);
        verify(credits, never()).awardRecordingValidated(any());
        verify(recordings, never()).save(any());
        assertThat(stored).anySatisfy(r -> assertThat(r.getGold()).isTrue());
    }

    @Test
    void trapOnRejectedRecordingExpectsAReject() {
        settledUnanimously(RecordingStatus.REJECTED, Verdict.REJECT);
        stubTrapStats(1, 1);

        submit(Verdict.REJECT);

        verify(credits).awardTrapCorrect(recording, me);
    }

    @Test
    void wrongTrapAnswerIsNotPaid() {
        settledUnanimously(RecordingStatus.VALIDATED, Verdict.ACCEPT);
        stubTrapStats(1, 0);

        submit(Verdict.REJECT);

        verify(credits, never()).awardTrapCorrect(any(), any());
    }

    @Test
    void settledRecordingWithoutUnanimityIsNotAUsableTrap() {
        recording.setStatus(RecordingStatus.VALIDATED);
        stored.add(earlier(30L, Verdict.ACCEPT, false));
        stored.add(earlier(31L, Verdict.ACCEPT, false));
        stored.add(earlier(32L, Verdict.REJECT, false));

        assertStatus(() -> submit(Verdict.ACCEPT), HttpStatus.CONFLICT);
        verify(reviews, never()).save(any());
    }

    // ---- Fiabilite des validateurs -------------------------------------------------------------------------------

    @Test
    void reliabilityBelowSeventyPercentRevokesTheQualification() {
        settledUnanimously(RecordingStatus.VALIDATED, Verdict.ACCEPT);
        stubTrapStats(5, 3);

        submit(Verdict.REJECT);

        assertThat(myQualification.getStatus()).isEqualTo(QualificationStatus.REVOKED);
        assertThat(myQualification.getScore()).isEqualTo(60.0f);
        verify(qualifications).save(myQualification);
    }

    @Test
    void exactlySeventyPercentKeepsTheQualification() {
        settledUnanimously(RecordingStatus.VALIDATED, Verdict.ACCEPT);
        stubTrapStats(10, 7);

        submit(Verdict.ACCEPT);

        assertThat(myQualification.getStatus()).isEqualTo(QualificationStatus.QUALIFIED);
        assertThat(myQualification.getScore()).isEqualTo(70.0f);
    }

    @Test
    void tooFewTrapsAreNotEnoughToRevoke() {
        // 4 pieges seulement, tous rates : l'echantillon est trop petit pour juger.
        settledUnanimously(RecordingStatus.VALIDATED, Verdict.ACCEPT);
        stubTrapStats(4, 0);

        submit(Verdict.REJECT);

        assertThat(myQualification.getStatus()).isEqualTo(QualificationStatus.QUALIFIED);
        assertThat(myQualification.getScore()).isEqualTo(0.0f);
    }
}
