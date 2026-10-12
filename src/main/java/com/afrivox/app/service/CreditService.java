package com.afrivox.app.service;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.CreditRate;
import com.afrivox.app.domain.CreditRateHistory;
import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.domain.Recording;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.CreditRateKind;
import com.afrivox.app.domain.enumeration.CreditReason;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.repository.CreditRateHistoryRepository;
import com.afrivox.app.repository.CreditRateRepository;
import com.afrivox.app.repository.CreditTransactionRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credits des contributeurs.
 *
 * <ul>
 *   <li><b>Registre</b> : {@code credit_transaction} n'est qu'ajoute (jamais modifie) ; le solde est la somme des
 *       lignes, calculee a la demande. Un meme enregistrement ne rapporte qu'une fois par personne et par motif
 *       (contrainte d'unicite en base + verification ici), donc rejouer un consensus ne paie pas deux fois.</li>
 *   <li><b>Tarifs</b> : trois montants globaux reglables par l'admin ({@link CreditRateKind}), que chaque campagne
 *       peut surcharger (colonne nulle = tarif global). Chaque changement est journalise.</li>
 *   <li><b>Moment du tarif</b> : le gain d'un enregistrement est fige a l'envoi ({@code recording.reward_credits}) ;
 *       celui d'un avis est celui en vigueur au moment du consensus.</li>
 *   <li>Les credits sont des entiers abstraits : la conversion en argent n'existe pas encore (retrait reporte).</li>
 * </ul>
 */
@Service
// Pas readOnly : mine() et myHistory() passent par getOrCreateCurrent(), qui peut creer le profil (INSERT).
@Transactional
public class CreditService {

    private static final Logger LOG = LoggerFactory.getLogger(CreditService.class);

    private static final String ENTITY_NAME = "credit";

    /** Garde-fou contre une faute de frappe : un tarif depasse rarement quelques dizaines de credits. */
    public static final int MAX_RATE = 1000;

    public static final int MAX_ADJUSTMENT = 100_000;

    private static final int MAX_NOTE = 140;

    /** Valeurs de depart (identiques a celles inserees par Liquibase) si une ligne manquait. */
    private static final Map<CreditRateKind, Integer> DEFAULTS = Map.of(
        CreditRateKind.VALIDATED_RECORDING,
        10,
        CreditRateKind.REVIEW_AGREED,
        1,
        CreditRateKind.TRAP_CORRECT,
        1
    );

    /** Tarif global d'un type de gain. */
    public record RateView(CreditRateKind kind, int amount, Instant updatedAt, String updatedBy) {}

    /** Tarifs propres a une campagne ({@code null} = tarif global) et tarifs effectivement appliques. */
    public record CampaignRates(
        Long campaignId,
        Integer validatedRecording,
        Integer reviewAgreed,
        Integer trapCorrect,
        Map<CreditRateKind, Integer> effective
    ) {}

    public record RateChangeView(
        Long id,
        String scope,
        CreditRateKind kind,
        Integer oldAmount,
        Integer newAmount,
        String changedBy,
        Instant changedAt
    ) {}

    /** Resume pour le contributeur : solde et part de chaque source (rien n'est stocke en double). */
    public record Summary(long balance, long earnedFromRecordings, long earnedFromReviews, Map<CreditRateKind, Integer> rates) {}

    public record TransactionView(Long id, int amount, CreditReason reason, Instant createdAt, Long recordingId, String note) {}

    private final CreditTransactionRepository transactionRepository;
    private final CreditRateRepository rateRepository;
    private final CreditRateHistoryRepository historyRepository;
    private final CampaignRepository campaignRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    public CreditService(
        CreditTransactionRepository transactionRepository,
        CreditRateRepository rateRepository,
        CreditRateHistoryRepository historyRepository,
        CampaignRepository campaignRepository,
        UserProfileRepository userProfileRepository,
        UserProfileService userProfileService
    ) {
        this.transactionRepository = transactionRepository;
        this.rateRepository = rateRepository;
        this.historyRepository = historyRepository;
        this.campaignRepository = campaignRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
    }

    // ---- Tarifs --------------------------------------------------------------------------------------------------

    /** Tarif global d'un type de gain. */
    public int globalRate(CreditRateKind kind) {
        return rateRepository.findById(kind).map(CreditRate::getAmount).orElse(DEFAULTS.get(kind));
    }

    /** Tarif applique a une campagne : sa surcharge si elle en a une, sinon le tarif global. */
    public int effectiveRate(Campaign campaign, CreditRateKind kind) {
        Integer override = campaign == null ? null : override(campaign, kind);
        return override != null ? override : globalRate(kind);
    }

    /** Ce que rapportera un enregistrement envoye maintenant dans cette campagne (fige sur la ligne). */
    public int rewardForNewRecording(Campaign campaign) {
        return effectiveRate(campaign, CreditRateKind.VALIDATED_RECORDING);
    }

    // ---- Versements (appeles par ValidationService au consensus) -------------------------------------------------

    /** L'enregistrement est valide : son auteur recoit le tarif fige a l'envoi (ou celui du moment pour un ancien). */
    public void awardRecordingValidated(Recording recording) {
        Campaign campaign = campaignOf(recording);
        int amount =
            recording.getRewardCredits() != null
                ? recording.getRewardCredits()
                : effectiveRate(campaign, CreditRateKind.VALIDATED_RECORDING);
        award(recording.getContributor(), CreditReason.RECORDING_VALIDATED, amount, recording.getId());
    }

    /** Un validateur a rendu un avis concordant avec le consensus. */
    public void awardReviewAgreed(Recording recording, UserProfile reviewer) {
        int amount = effectiveRate(campaignOf(recording), CreditRateKind.REVIEW_AGREED);
        award(reviewer, CreditReason.REVIEW_DONE, amount, recording.getId());
    }

    /** Un validateur a reussi une tache piege. */
    public void awardTrapCorrect(Recording recording, UserProfile reviewer) {
        int amount = effectiveRate(campaignOf(recording), CreditRateKind.TRAP_CORRECT);
        award(reviewer, CreditReason.TRAP_CORRECT, amount, recording.getId());
    }

    private void award(UserProfile profile, CreditReason reason, int amount, Long recordingId) {
        if (amount <= 0) {
            return;
        }
        if (transactionRepository.existsByProfileIdAndReasonAndRecordingId(profile.getId(), reason, recordingId)) {
            return;
        }
        CreditTransaction tx = new CreditTransaction();
        tx.setProfile(profile);
        tx.setReason(reason);
        tx.setAmount(amount);
        tx.setRecordingId(recordingId);
        tx.setCreatedAt(Instant.now());
        transactionRepository.save(tx);
        LOG.debug("Credits : +{} ({}) pour le profil {}, enregistrement {}", amount, reason, profile.getId(), recordingId);
    }

    // ---- Contributeur --------------------------------------------------------------------------------------------

    public Summary mine() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        long balance = transactionRepository.balanceOf(me.getId());
        long recordings = 0;
        long reviews = 0;
        for (Object[] row : transactionRepository.earnedByReason(me.getId())) {
            CreditReason reason = (CreditReason) row[0];
            long sum = ((Number) row[1]).longValue();
            if (reason == CreditReason.RECORDING_VALIDATED) {
                recordings += sum;
            } else if (reason == CreditReason.REVIEW_DONE || reason == CreditReason.TRAP_CORRECT) {
                reviews += sum;
            }
        }
        return new Summary(balance, recordings, reviews, globalRates());
    }

    public Page<TransactionView> myHistory(Pageable pageable) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        return transactionRepository
            .findAllByProfileId(me.getId(), pageable)
            .map(t -> new TransactionView(t.getId(), t.getAmount(), t.getReason(), t.getCreatedAt(), t.getRecordingId(), t.getNote()));
    }

    // ---- Administration ------------------------------------------------------------------------------------------

    public List<RateView> rates() {
        Map<CreditRateKind, CreditRate> stored = new EnumMap<>(CreditRateKind.class);
        rateRepository.findAll().forEach(r -> stored.put(r.getKind(), r));
        List<RateView> views = new ArrayList<>();
        for (CreditRateKind kind : CreditRateKind.values()) {
            CreditRate rate = stored.get(kind);
            views.add(
                rate == null
                    ? new RateView(kind, DEFAULTS.get(kind), null, null)
                    : new RateView(kind, rate.getAmount(), rate.getUpdatedAt(), rate.getUpdatedBy())
            );
        }
        return views;
    }

    /** Change les tarifs globaux ; seuls les montants qui different sont ecrits et journalises. */
    public List<RateView> updateRates(Map<CreditRateKind, Integer> amounts) {
        String admin = currentLogin();
        Instant now = Instant.now();
        for (Map.Entry<CreditRateKind, Integer> entry : amounts.entrySet()) {
            Integer amount = entry.getValue();
            requireValidRate(amount, false);
            CreditRate rate = rateRepository
                .findById(entry.getKey())
                .orElseGet(() -> new CreditRate(entry.getKey(), DEFAULTS.get(entry.getKey()), now, admin));
            if (rate.getAmount() == amount) {
                continue;
            }
            int previous = rate.getAmount();
            rate.change(amount, now, admin);
            rateRepository.save(rate);
            historyRepository.save(new CreditRateHistory("GLOBAL", entry.getKey(), previous, amount, admin, now));
            LOG.info("Tarif global {} : {} -> {} (par {})", entry.getKey(), previous, amount, admin);
        }
        return rates();
    }

    public Page<RateChangeView> history(Pageable pageable) {
        return historyRepository
            .findAll(pageable)
            .map(h ->
                new RateChangeView(
                    h.getId(),
                    h.getScope(),
                    h.getKind(),
                    h.getOldAmount(),
                    h.getNewAmount(),
                    h.getChangedBy(),
                    h.getChangedAt()
                )
            );
    }

    public CampaignRates campaignRates(Long campaignId) {
        return toCampaignRates(findCampaign(campaignId));
    }

    /**
     * Fixe les tarifs propres a une campagne ; {@code null} = retour au tarif global. Les trois valeurs sont
     * remplacees ensemble (un formulaire complet), seuls les changements sont journalises.
     */
    public CampaignRates setCampaignRates(Long campaignId, Integer validatedRecording, Integer reviewAgreed, Integer trapCorrect) {
        requireValidRate(validatedRecording, true);
        requireValidRate(reviewAgreed, true);
        requireValidRate(trapCorrect, true);
        Campaign campaign = findCampaign(campaignId);
        String admin = currentLogin();
        Instant now = Instant.now();
        String scope = "CAMPAIGN:" + campaignId;

        logChange(scope, CreditRateKind.VALIDATED_RECORDING, campaign.getCreditValidatedRecording(), validatedRecording, admin, now);
        logChange(scope, CreditRateKind.REVIEW_AGREED, campaign.getCreditReviewAgreed(), reviewAgreed, admin, now);
        logChange(scope, CreditRateKind.TRAP_CORRECT, campaign.getCreditTrapCorrect(), trapCorrect, admin, now);

        campaign.setCreditValidatedRecording(validatedRecording);
        campaign.setCreditReviewAgreed(reviewAgreed);
        campaign.setCreditTrapCorrect(trapCorrect);
        campaignRepository.save(campaign);
        return toCampaignRates(campaign);
    }

    /**
     * Correction manuelle (fraude, erreur) : ligne du registre avec motif obligatoire. Refusee si elle rendrait le
     * solde negatif. Le compte est designe par son login.
     */
    public TransactionView adjust(String login, int amount, String note) {
        if (amount == 0 || Math.abs(amount) > MAX_ADJUSTMENT) {
            throw new BadRequestAlertException("Montant invalide", ENTITY_NAME, "invalidamount");
        }
        if (note == null || note.isBlank() || note.trim().length() > MAX_NOTE) {
            throw new BadRequestAlertException("Motif obligatoire (140 caracteres au plus)", ENTITY_NAME, "invalidnote");
        }
        UserProfile profile = userProfileRepository
            .findOneByUserLogin(login == null ? "" : login.trim())
            .orElseThrow(() -> new BadRequestAlertException("Compte introuvable", ENTITY_NAME, "usernotfound"));
        if (transactionRepository.balanceOf(profile.getId()) + amount < 0) {
            throw new BadRequestAlertException("Le solde ne peut pas devenir negatif", ENTITY_NAME, "balancenegative");
        }
        String admin = currentLogin();
        CreditTransaction tx = new CreditTransaction();
        tx.setProfile(profile);
        tx.setReason(CreditReason.ADJUSTMENT);
        tx.setAmount(amount);
        // L'auteur de l'ajustement est garde avec le motif : le registre est la seule trace.
        tx.setNote(note.trim() + " [" + admin + "]");
        tx.setCreatedAt(Instant.now());
        transactionRepository.save(tx);
        LOG.info("Ajustement de {} credits pour {} par {} : {}", amount, login, admin, note.trim());
        return new TransactionView(tx.getId(), amount, tx.getReason(), tx.getCreatedAt(), null, tx.getNote());
    }

    // ---- Interne -------------------------------------------------------------------------------------------------

    private Map<CreditRateKind, Integer> globalRates() {
        Map<CreditRateKind, Integer> map = new EnumMap<>(CreditRateKind.class);
        rates().forEach(r -> map.put(r.kind(), r.amount()));
        return map;
    }

    private CampaignRates toCampaignRates(Campaign c) {
        Map<CreditRateKind, Integer> effective = new EnumMap<>(CreditRateKind.class);
        for (CreditRateKind kind : CreditRateKind.values()) {
            effective.put(kind, effectiveRate(c, kind));
        }
        return new CampaignRates(
            c.getId(),
            c.getCreditValidatedRecording(),
            c.getCreditReviewAgreed(),
            c.getCreditTrapCorrect(),
            effective
        );
    }

    private void logChange(String scope, CreditRateKind kind, Integer oldAmount, Integer newAmount, String admin, Instant now) {
        if (java.util.Objects.equals(oldAmount, newAmount)) {
            return;
        }
        historyRepository.save(new CreditRateHistory(scope, kind, oldAmount, newAmount, admin, now));
        LOG.info("Tarif {} {} : {} -> {} (par {})", scope, kind, oldAmount, newAmount, admin);
    }

    private Campaign findCampaign(Long id) {
        return campaignRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Campagne introuvable", ENTITY_NAME, "campaignnotfound"));
    }

    private static Campaign campaignOf(Recording recording) {
        return recording.getPrompt() == null ? null : recording.getPrompt().getCampaign();
    }

    private static Integer override(Campaign campaign, CreditRateKind kind) {
        return switch (kind) {
            case VALIDATED_RECORDING -> campaign.getCreditValidatedRecording();
            case REVIEW_AGREED -> campaign.getCreditReviewAgreed();
            case TRAP_CORRECT -> campaign.getCreditTrapCorrect();
        };
    }

    private static void requireValidRate(Integer amount, boolean nullAllowed) {
        if (amount == null ? !nullAllowed : amount < 0 || amount > MAX_RATE) {
            throw new BadRequestAlertException("Tarif invalide (0 a " + MAX_RATE + ")", ENTITY_NAME, "invalidrate");
        }
    }

    private static String currentLogin() {
        String login = SecurityUtils.getCurrentUserLogin().orElse("system");
        return login.length() > 50 ? login.substring(0, 50) : login;
    }
}
