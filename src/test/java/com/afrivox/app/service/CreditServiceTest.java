package com.afrivox.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.CreditRate;
import com.afrivox.app.domain.CreditRateHistory;
import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.CreditRateKind;
import com.afrivox.app.domain.enumeration.CreditReason;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.repository.CreditRateHistoryRepository;
import com.afrivox.app.repository.CreditRateRepository;
import com.afrivox.app.repository.CreditTransactionRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Registre de credits : tarifs globaux et par campagne, versements idempotents, ajustements de l'admin. */
class CreditServiceTest {

    private CreditTransactionRepository transactions;
    private CreditRateRepository rates;
    private CreditRateHistoryRepository history;
    private CampaignRepository campaigns;
    private UserProfileRepository profiles;
    private UserProfileService profileService;
    private CreditService service;

    @BeforeEach
    void setUp() {
        transactions = mock(CreditTransactionRepository.class);
        rates = mock(CreditRateRepository.class);
        history = mock(CreditRateHistoryRepository.class);
        campaigns = mock(CampaignRepository.class);
        profiles = mock(UserProfileRepository.class);
        profileService = mock(UserProfileService.class);
        service = new CreditService(transactions, rates, history, campaigns, profiles, profileService);
        when(rates.findById(any())).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "n/a"));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ---- Aides ---------------------------------------------------------------------------------------------------

    private void globalRate(CreditRateKind kind, int amount) {
        when(rates.findById(kind)).thenReturn(Optional.of(new CreditRate(kind, amount, Instant.now(), "admin")));
    }

    private static UserProfile profile(long id) {
        return new UserProfile().id(id);
    }

    private static Campaign campaign(Integer validated, Integer review, Integer trap) {
        Campaign c = new Campaign();
        c.setId(7L);
        c.setCreditValidatedRecording(validated);
        c.setCreditReviewAgreed(review);
        c.setCreditTrapCorrect(trap);
        return c;
    }

    private static Recording recording(long id, UserProfile author, Campaign campaign) {
        Prompt prompt = new Prompt();
        prompt.setCampaign(campaign);
        Recording r = new Recording().id(id);
        r.setContributor(author);
        r.setPrompt(prompt);
        return r;
    }

    private CreditTransaction savedTransaction() {
        ArgumentCaptor<CreditTransaction> captor = ArgumentCaptor.forClass(CreditTransaction.class);
        verify(transactions).save(captor.capture());
        return captor.getValue();
    }

    private static void assertBadRequest(Runnable action, String errorKey) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BadRequestAlertException.class, e ->
            assertThat(e.getErrorKey()).isEqualTo(errorKey)
        );
    }

    // ---- Tarifs --------------------------------------------------------------------------------------------------

    @Test
    void globalRateFallsBackToStartingValuesWhenRowIsMissing() {
        assertThat(service.globalRate(CreditRateKind.VALIDATED_RECORDING)).isEqualTo(10);
        assertThat(service.globalRate(CreditRateKind.REVIEW_AGREED)).isEqualTo(1);
        assertThat(service.globalRate(CreditRateKind.TRAP_CORRECT)).isEqualTo(1);
    }

    @Test
    void globalRateReadsTheStoredAmount() {
        globalRate(CreditRateKind.VALIDATED_RECORDING, 25);

        assertThat(service.globalRate(CreditRateKind.VALIDATED_RECORDING)).isEqualTo(25);
    }

    @Test
    void campaignOverrideWinsOverGlobalRate() {
        globalRate(CreditRateKind.VALIDATED_RECORDING, 10);

        assertThat(service.effectiveRate(campaign(40, null, null), CreditRateKind.VALIDATED_RECORDING)).isEqualTo(40);
    }

    @Test
    void campaignWithoutOverrideUsesGlobalRate() {
        globalRate(CreditRateKind.REVIEW_AGREED, 3);

        assertThat(service.effectiveRate(campaign(40, null, null), CreditRateKind.REVIEW_AGREED)).isEqualTo(3);
        assertThat(service.effectiveRate(null, CreditRateKind.REVIEW_AGREED)).isEqualTo(3);
    }

    @Test
    void zeroOverrideIsARealOverrideNotAMissingOne() {
        // Une campagne peut volontairement ne rien rapporter : 0 ne doit pas retomber sur le tarif global.
        globalRate(CreditRateKind.VALIDATED_RECORDING, 10);

        assertThat(service.effectiveRate(campaign(0, null, null), CreditRateKind.VALIDATED_RECORDING)).isZero();
    }

    // ---- Versements ----------------------------------------------------------------------------------------------

    @Test
    void validatedRecordingPaysItsAuthorTheRateFrozenAtUpload() {
        // Le tarif global a monte depuis l'envoi : l'auteur touche ce qui etait annonce quand il a enregistre.
        globalRate(CreditRateKind.VALIDATED_RECORDING, 50);
        Recording recording = recording(100L, profile(1L), campaign(null, null, null));
        recording.setRewardCredits(10);

        service.awardRecordingValidated(recording);

        CreditTransaction tx = savedTransaction();
        assertThat(tx.getAmount()).isEqualTo(10);
        assertThat(tx.getReason()).isEqualTo(CreditReason.RECORDING_VALIDATED);
        assertThat(tx.getRecordingId()).isEqualTo(100L);
        assertThat(tx.getProfile().getId()).isEqualTo(1L);
    }

    @Test
    void recordingWithoutFrozenRewardUsesTheCurrentRate() {
        globalRate(CreditRateKind.VALIDATED_RECORDING, 12);
        Recording recording = recording(100L, profile(1L), campaign(null, null, null));

        service.awardRecordingValidated(recording);

        assertThat(savedTransaction().getAmount()).isEqualTo(12);
    }

    @Test
    void sameRecordingNeverPaysTwice() {
        Recording recording = recording(100L, profile(1L), campaign(null, null, null));
        when(transactions.existsByProfileIdAndReasonAndRecordingId(1L, CreditReason.RECORDING_VALIDATED, 100L)).thenReturn(true);

        service.awardRecordingValidated(recording);

        verify(transactions, never()).save(any());
    }

    @Test
    void zeroRatePaysNothing() {
        Recording recording = recording(100L, profile(1L), campaign(null, 0, null));

        service.awardReviewAgreed(recording, profile(2L));

        verify(transactions, never()).save(any());
    }

    @Test
    void agreedReviewIsPaidAtTheCampaignRate() {
        globalRate(CreditRateKind.REVIEW_AGREED, 1);
        Recording recording = recording(100L, profile(1L), campaign(null, 4, null));

        service.awardReviewAgreed(recording, profile(2L));

        CreditTransaction tx = savedTransaction();
        assertThat(tx.getAmount()).isEqualTo(4);
        assertThat(tx.getReason()).isEqualTo(CreditReason.REVIEW_DONE);
        assertThat(tx.getProfile().getId()).isEqualTo(2L);
    }

    @Test
    void correctTrapIsPaidWithItsOwnReason() {
        globalRate(CreditRateKind.TRAP_CORRECT, 2);
        Recording recording = recording(100L, profile(1L), campaign(null, null, null));

        service.awardTrapCorrect(recording, profile(2L));

        CreditTransaction tx = savedTransaction();
        assertThat(tx.getAmount()).isEqualTo(2);
        assertThat(tx.getReason()).isEqualTo(CreditReason.TRAP_CORRECT);
    }

    // ---- Contributeur --------------------------------------------------------------------------------------------

    @Test
    void summarySplitsEarningsBySource() {
        when(profileService.getOrCreateCurrent()).thenReturn(profile(1L));
        when(transactions.balanceOf(1L)).thenReturn(35L);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { CreditReason.RECORDING_VALIDATED, 30L });
        rows.add(new Object[] { CreditReason.REVIEW_DONE, 4L });
        rows.add(new Object[] { CreditReason.TRAP_CORRECT, 2L });
        rows.add(new Object[] { CreditReason.ADJUSTMENT, 5L });
        when(transactions.earnedByReason(1L)).thenReturn(rows);

        CreditService.Summary summary = service.mine();

        assertThat(summary.balance()).isEqualTo(35);
        assertThat(summary.earnedFromRecordings()).isEqualTo(30);
        // Pieges et avis concordants sont tous deux des gains de validateur ; un ajustement n'est une source ni de l'un ni de l'autre.
        assertThat(summary.earnedFromReviews()).isEqualTo(6);
        assertThat(summary.rates()).containsEntry(CreditRateKind.VALIDATED_RECORDING, 10);
    }

    // ---- Tarifs globaux (admin) ----------------------------------------------------------------------------------

    @Test
    void ratesListsEveryKindEvenWhenNothingIsStored() {
        assertThat(service.rates()).extracting(CreditService.RateView::kind).containsExactlyInAnyOrder(CreditRateKind.values());
    }

    @Test
    void updateRatesRejectsOutOfRangeAndMissingAmounts() {
        assertBadRequest(() -> service.updateRates(Map.of(CreditRateKind.VALIDATED_RECORDING, -1)), "invalidrate");
        assertBadRequest(() -> service.updateRates(Map.of(CreditRateKind.VALIDATED_RECORDING, CreditService.MAX_RATE + 1)), "invalidrate");
        Map<CreditRateKind, Integer> withNull = new EnumMap<>(CreditRateKind.class);
        withNull.put(CreditRateKind.REVIEW_AGREED, null);
        assertBadRequest(() -> service.updateRates(withNull), "invalidrate");

        verify(history, never()).save(any());
    }

    @Test
    void updateRatesAcceptsTheBounds() {
        service.updateRates(Map.of(CreditRateKind.VALIDATED_RECORDING, 0, CreditRateKind.REVIEW_AGREED, CreditService.MAX_RATE));

        verify(history, times(2)).save(any(CreditRateHistory.class));
    }

    @Test
    void updateRatesLogsOnlyRealChanges() {
        globalRate(CreditRateKind.VALIDATED_RECORDING, 10);
        globalRate(CreditRateKind.REVIEW_AGREED, 1);

        service.updateRates(Map.of(CreditRateKind.VALIDATED_RECORDING, 10, CreditRateKind.REVIEW_AGREED, 2));

        ArgumentCaptor<CreditRateHistory> captor = ArgumentCaptor.forClass(CreditRateHistory.class);
        verify(history).save(captor.capture());
        CreditRateHistory change = captor.getValue();
        assertThat(change.getScope()).isEqualTo("GLOBAL");
        assertThat(change.getKind()).isEqualTo(CreditRateKind.REVIEW_AGREED);
        assertThat(change.getOldAmount()).isEqualTo(1);
        assertThat(change.getNewAmount()).isEqualTo(2);
        assertThat(change.getChangedBy()).isEqualTo("admin");
    }

    // ---- Tarifs par campagne -------------------------------------------------------------------------------------

    @Test
    void setCampaignRatesStoresOverridesAndLogsChangesOnly() {
        Campaign campaign = campaign(20, null, 5);
        when(campaigns.findById(7L)).thenReturn(Optional.of(campaign));

        // validated inchange (20), review passe de global a 3, trap repasse au tarif global (null).
        CreditService.CampaignRates result = service.setCampaignRates(7L, 20, 3, null);

        assertThat(campaign.getCreditReviewAgreed()).isEqualTo(3);
        assertThat(campaign.getCreditTrapCorrect()).isNull();
        verify(campaigns).save(campaign);
        ArgumentCaptor<CreditRateHistory> captor = ArgumentCaptor.forClass(CreditRateHistory.class);
        verify(history, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
            .extracting(CreditRateHistory::getKind)
            .containsExactly(CreditRateKind.REVIEW_AGREED, CreditRateKind.TRAP_CORRECT);
        assertThat(captor.getAllValues()).allSatisfy(h -> assertThat(h.getScope()).isEqualTo("CAMPAIGN:7"));
        assertThat(result.effective()).containsEntry(CreditRateKind.REVIEW_AGREED, 3).containsEntry(CreditRateKind.VALIDATED_RECORDING, 20);
    }

    @Test
    void setCampaignRatesRejectsInvalidAmountsBeforeTouchingAnything() {
        assertBadRequest(() -> service.setCampaignRates(7L, -5, null, null), "invalidrate");
        assertBadRequest(() -> service.setCampaignRates(7L, null, CreditService.MAX_RATE + 1, null), "invalidrate");

        verify(campaigns, never()).save(any());
    }

    @Test
    void setCampaignRatesFailsForUnknownCampaign() {
        when(campaigns.findById(99L)).thenReturn(Optional.empty());

        assertBadRequest(() -> service.setCampaignRates(99L, 1, 1, 1), "campaignnotfound");
    }

    // ---- Ajustements manuels -------------------------------------------------------------------------------------

    @Test
    void adjustRejectsZeroAndExcessiveAmounts() {
        assertBadRequest(() -> service.adjust("amadou", 0, "motif"), "invalidamount");
        assertBadRequest(() -> service.adjust("amadou", CreditService.MAX_ADJUSTMENT + 1, "motif"), "invalidamount");
        assertBadRequest(() -> service.adjust("amadou", -CreditService.MAX_ADJUSTMENT - 1, "motif"), "invalidamount");
    }

    @Test
    void adjustRequiresAReasonOfReasonableLength() {
        assertBadRequest(() -> service.adjust("amadou", 5, null), "invalidnote");
        assertBadRequest(() -> service.adjust("amadou", 5, "   "), "invalidnote");
        assertBadRequest(() -> service.adjust("amadou", 5, "x".repeat(141)), "invalidnote");
    }

    @Test
    void adjustFailsForUnknownLogin() {
        when(profiles.findOneByUserLogin("inconnu")).thenReturn(Optional.empty());

        assertBadRequest(() -> service.adjust("inconnu", 5, "motif"), "usernotfound");
    }

    @Test
    void adjustCannotMakeTheBalanceNegative() {
        when(profiles.findOneByUserLogin("amadou")).thenReturn(Optional.of(profile(1L)));
        when(transactions.balanceOf(1L)).thenReturn(10L);

        assertBadRequest(() -> service.adjust("amadou", -11, "fraude"), "balancenegative");
        verify(transactions, never()).save(any());
    }

    @Test
    void adjustCanBringTheBalanceExactlyToZero() {
        when(profiles.findOneByUserLogin("amadou")).thenReturn(Optional.of(profile(1L)));
        when(transactions.balanceOf(1L)).thenReturn(10L);

        service.adjust("amadou", -10, "remise a zero");

        assertThat(savedTransaction().getAmount()).isEqualTo(-10);
    }

    @Test
    void adjustKeepsTheAdminAndReasonInTheLedger() {
        when(profiles.findOneByUserLogin("amadou")).thenReturn(Optional.of(profile(1L)));
        when(transactions.balanceOf(1L)).thenReturn(0L);

        CreditService.TransactionView view = service.adjust(" amadou ", 20, "  bonus de lancement ");

        CreditTransaction tx = savedTransaction();
        assertThat(tx.getReason()).isEqualTo(CreditReason.ADJUSTMENT);
        assertThat(tx.getAmount()).isEqualTo(20);
        assertThat(tx.getNote()).isEqualTo("bonus de lancement [admin]");
        assertThat(tx.getRecordingId()).isNull();
        assertThat(view.amount()).isEqualTo(20);
    }
}
