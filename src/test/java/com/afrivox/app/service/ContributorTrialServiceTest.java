package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ContributorStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Periode d'essai : plafond d'envois, promotion automatique, jamais de retrogradation automatique. */
class ContributorTrialServiceTest {

    private static final long PROFILE_ID = 5L;

    private RecordingRepository recordings;
    private UserProfileRepository profiles;
    private ContributorTrialService service;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        recordings = mock(RecordingRepository.class);
        profiles = mock(UserProfileRepository.class);
        ApplicationProperties properties = new ApplicationProperties();
        properties.getContributorTrial().setMaxRecordings(10);
        properties.getContributorTrial().setMinAcceptRate(0.7);
        service = new ContributorTrialService(properties, recordings, profiles, mock(UserProfileService.class));

        profile = new UserProfile().id(PROFILE_ID);
        profile.setContributorStatus(ContributorStatus.TRIAL);
    }

    private void counts(long submitted, long validated, long rejected) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { RecordingStatus.SUBMITTED, submitted });
        rows.add(new Object[] { RecordingStatus.VALIDATED, validated });
        rows.add(new Object[] { RecordingStatus.REJECTED, rejected });
        when(recordings.countByContributorAndStatus(PROFILE_ID)).thenReturn(rows);
    }

    private static void assertErrorKey(Runnable action, String key) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(key)
        );
    }

    // ---- Plafond d'envois ----------------------------------------------------------------------------------------

    @Test
    void trialContributorCanUploadBelowTheCap() {
        counts(3, 3, 3);

        assertThatCode(() -> service.assertCanUpload(profile)).doesNotThrowAnyException();
    }

    @Test
    void trialContributorIsStoppedAtTheCap() {
        counts(2, 4, 4);

        assertErrorKey(() -> service.assertCanUpload(profile), "triallimit");
    }

    @Test
    void rejectedRecordingsStillCountTowardsTheCap() {
        // Un rejet ne libere pas de place : sinon on pourrait enregistrer n'importe quoi a l'infini.
        counts(0, 0, 10);

        assertErrorKey(() -> service.assertCanUpload(profile), "triallimit");
    }

    @Test
    void suspendedContributorCannotUpload() {
        profile.setContributorStatus(ContributorStatus.SUSPENDED);

        assertErrorKey(() -> service.assertCanUpload(profile), "contributorsuspended");
    }

    @Test
    void activeContributorHasNoCap() {
        profile.setContributorStatus(ContributorStatus.ACTIVE);
        counts(500, 500, 500);

        assertThatCode(() -> service.assertCanUpload(profile)).doesNotThrowAnyException();
    }

    // ---- Promotion automatique -----------------------------------------------------------------------------------

    @Test
    void goodTrialPromotesTheContributor() {
        counts(0, 8, 2);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.ACTIVE);
        verify(profiles).save(profile);
    }

    @Test
    void exactlyTheMinimumRateIsEnough() {
        counts(0, 7, 3);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.ACTIVE);
    }

    @Test
    void poorTrialKeepsTheContributorInTrialForTheAdminToDecide() {
        counts(0, 6, 4);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.TRIAL);
        verify(profiles, never()).save(any());
    }

    @Test
    void nothingHappensWhileSomeRecordingsAreStillPending() {
        counts(1, 9, 0);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.TRIAL);
    }

    @Test
    void evaluationNeverDemotesAnActiveContributor() {
        profile.setContributorStatus(ContributorStatus.ACTIVE);
        counts(0, 0, 10);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.ACTIVE);
        verify(profiles, never()).save(any());
    }

    @Test
    void evaluationNeverLiftsASuspension() {
        profile.setContributorStatus(ContributorStatus.SUSPENDED);
        counts(0, 10, 0);

        service.evaluate(profile);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.SUSPENDED);
    }

    // ---- Decision de l'admin -------------------------------------------------------------------------------------

    @Test
    void adminCanChangeTheStatus() {
        when(profiles.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        service.setStatus(PROFILE_ID, ContributorStatus.SUSPENDED);

        assertThat(profile.getContributorStatus()).isEqualTo(ContributorStatus.SUSPENDED);
        verify(profiles).save(profile);
    }

    @Test
    void changingTheStatusOfAnUnknownContributorFails() {
        when(profiles.findById(99L)).thenReturn(Optional.empty());

        assertErrorKey(() -> service.setStatus(99L, ContributorStatus.ACTIVE), "notfound");
    }
}
