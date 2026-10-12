package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.LanguageQualification;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validation par les pairs.
 *
 * <ul>
 *   <li><b>Qualification</b> : un contributeur demande a valider une langue (PENDING) ; l'admin l'accepte (QUALIFIED).</li>
 *   <li><b>Consensus</b> : chaque enregistrement est relu par au plus {@value #REVIEWS_PER_RECORDING} validateurs
 *       qualifies (jamais son auteur, jamais deux fois le meme). Des que {@value #AGREEMENT} avis concordent, il est
 *       VALIDATED ou REJECTED.</li>
 *   <li><b>Taches pieges</b> : de temps en temps on propose a un validateur un enregistrement deja tranche a
 *       l'unanimite, sans le lui dire. Son avis ne compte pas dans le consensus mais mesure sa fiabilite ; trop
 *       d'erreurs revoquent sa qualification.</li>
 * </ul>
 */
@Service
@Transactional
public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    private static final String ENTITY_NAME = "recordingReview";

    static final int REVIEWS_PER_RECORDING = 3;
    static final int AGREEMENT = 2;
    /** Une tache sur cinq environ est un piege. */
    static final double TRAP_RATE = 0.2;
    /** En dessous de ce nombre de pieges, on ne juge pas encore la fiabilite. */
    static final int TRAP_MIN_SAMPLE = 5;
    static final double TRAP_MIN_ACCURACY = 0.7;

    public record TaskView(Long recordingId, String sentence, Integer durationMs) {}

    public record QualificationView(Long id, String languageCode, String languageName, QualificationStatus status, Float score) {}

    public record PendingQualification(Long id, String displayName, String languageCode, String languageName) {}

    public record ReviewInput(
        Long recordingId,
        Verdict verdict,
        Boolean audioOk,
        Boolean pronunciationOk,
        Boolean transcriptOk,
        String note
    ) {}

    /** Fichier audio a servir et son type. */
    public record AudioFile(Path file, String contentType) {}

    private final ApplicationProperties.Audio audioProperties;
    private final RecordingRepository recordingRepository;
    private final RecordingReviewRepository reviewRepository;
    private final LanguageQualificationRepository qualificationRepository;
    private final LanguageRepository languageRepository;
    private final LanguageActivationService activationService;
    private final UserProfileService userProfileService;
    private final ContributorTrialService trialService;
    private final CreditService creditService;

    public ValidationService(
        ApplicationProperties applicationProperties,
        RecordingRepository recordingRepository,
        RecordingReviewRepository reviewRepository,
        LanguageQualificationRepository qualificationRepository,
        LanguageRepository languageRepository,
        LanguageActivationService activationService,
        UserProfileService userProfileService,
        ContributorTrialService trialService,
        CreditService creditService
    ) {
        this.trialService = trialService;
        this.creditService = creditService;
        this.audioProperties = applicationProperties.getAudio();
        this.recordingRepository = recordingRepository;
        this.reviewRepository = reviewRepository;
        this.qualificationRepository = qualificationRepository;
        this.languageRepository = languageRepository;
        this.activationService = activationService;
        this.userProfileService = userProfileService;
    }

    // ---- Qualification -------------------------------------------------------------------------------------------

    public QualificationView requestQualification(String languageCode) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Language language = languageRepository
            .findOneByCode(languageCode)
            .orElseThrow(() -> new BadRequestAlertException("Langue introuvable", ENTITY_NAME, "languagenotfound"));
        LanguageQualification q = qualificationRepository.findOneByProfileIdAndLanguageId(me.getId(), language.getId()).orElseGet(() -> {
            LanguageQualification created = new LanguageQualification();
            created.setProfile(me);
            created.setLanguage(language);
            created.setStatus(QualificationStatus.PENDING);
            return qualificationRepository.save(created);
        });
        return toView(q);
    }

    public List<QualificationView> myQualifications() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        return qualificationRepository.findAllByProfileIdWithLanguage(me.getId()).stream().map(ValidationService::toView).toList();
    }

    public List<PendingQualification> pendingQualifications() {
        return qualificationRepository
            .findAllByStatusWithDetails(QualificationStatus.PENDING)
            .stream()
            .map(q ->
                new PendingQualification(q.getId(), q.getProfile().getDisplayName(), q.getLanguage().getCode(), q.getLanguage().getName())
            )
            .toList();
    }

    /** Decision de l'admin : accepter (QUALIFIED) ou refuser/retirer (REVOKED). Accepter peut activer la langue. */
    public void decide(Long qualificationId, boolean approve) {
        LanguageQualification q = qualificationRepository
            .findById(qualificationId)
            .orElseThrow(() -> new BadRequestAlertException("Qualification introuvable", ENTITY_NAME, "qualificationnotfound"));
        if (approve) {
            q.setStatus(QualificationStatus.QUALIFIED);
            q.setQualifiedAt(Instant.now());
            qualificationRepository.save(q);
            activationService.promoteIfReady(q.getLanguage().getId());
        } else {
            q.setStatus(QualificationStatus.REVOKED);
            qualificationRepository.save(q);
        }
    }

    // ---- Relecture -----------------------------------------------------------------------------------------------

    /** Prochaine tache pour moi dans cette langue ; vide s'il n'y a plus rien a relire. */
    public Optional<TaskView> next(String languageCode) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Language language = languageOf(languageCode);
        requireQualified(me, language);

        if (ThreadLocalRandom.current().nextDouble() < TRAP_RATE) {
            Optional<Recording> trap = recordingRepository
                .findSettledForTrap(language.getId(), me.getId(), PageRequest.of(0, 20))
                .stream()
                .filter(this::unanimous)
                .findAny();
            if (trap.isPresent()) {
                return trap.map(ValidationService::toTask);
            }
        }
        return recordingRepository
            .findToReview(language.getId(), me.getId(), REVIEWS_PER_RECORDING, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(ValidationService::toTask);
    }

    /** Audio d'un enregistrement a relire : reserve aux validateurs qualifies de sa langue, jamais a son auteur. */
    public AudioFile audio(Long recordingId) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Recording recording = recordingRepository
            .findById(recordingId)
            .filter(r -> r.getStatus() != RecordingStatus.WITHDRAWN)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        requireQualified(me, languageOfRecording(recording));
        if (recording.getContributor().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Path root = Path.of(audioProperties.getStorageDir()).toAbsolutePath().normalize();
        Path file = root.resolve(recording.getAudioPath()).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return new AudioFile(file, contentType(recording.getAudioPath()));
    }

    /** Enregistre un avis. Les pieges ne comptent pas dans le consensus : ils mesurent la fiabilite du validateur. */
    public void submit(ReviewInput input) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Recording recording = recordingRepository
            .findById(input.recordingId())
            .orElseThrow(() -> new BadRequestAlertException("Enregistrement introuvable", ENTITY_NAME, "recordingnotfound"));
        Language language = languageOfRecording(recording);
        requireQualified(me, language);
        if (recording.getContributor().getId().equals(me.getId())) {
            throw new BadRequestAlertException("On ne relit pas ses propres enregistrements", ENTITY_NAME, "ownrecording");
        }
        if (reviewRepository.existsByRecordingIdAndReviewerId(recording.getId(), me.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deja relu");
        }

        boolean trap = recording.getStatus() != RecordingStatus.SUBMITTED;
        if (trap && !unanimous(recording)) {
            // Tranche entre-temps, sans unanimite : ce n'est pas un piege fiable, l'avis n'est pas retenu.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deja tranche");
        }
        if (
            !trap &&
            reviewRepository
                .findAllByRecordingId(recording.getId())
                .stream()
                .filter(r -> !Boolean.TRUE.equals(r.getGold()))
                .count() >= REVIEWS_PER_RECORDING
        ) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deja assez de relectures");
        }

        RecordingReview review = new RecordingReview();
        review.setRecording(recording);
        review.setReviewer(me);
        review.setVerdict(input.verdict());
        review.setAudioOk(input.audioOk());
        review.setPronunciationOk(input.pronunciationOk());
        review.setTranscriptOk(input.transcriptOk());
        review.setNote(input.note());
        review.setGold(trap);
        review.setCreatedAt(Instant.now());
        reviewRepository.save(review);

        if (trap) {
            updateReliability(me, language);
            // Piege reussi : l'avis va dans le sens du consensus deja acquis (ACCEPT pour VALIDATED, REJECT sinon).
            Verdict expected = recording.getStatus() == RecordingStatus.VALIDATED ? Verdict.ACCEPT : Verdict.REJECT;
            if (input.verdict() == expected) {
                creditService.awardTrapCorrect(recording, me);
            }
        } else {
            settle(recording);
        }
    }

    // ---- Interne -------------------------------------------------------------------------------------------------

    /** Applique la regle de consensus : {@value #AGREEMENT} avis identiques tranchent. */
    private void settle(Recording recording) {
        List<RecordingReview> reviews = reviewRepository
            .findAllByRecordingId(recording.getId())
            .stream()
            .filter(r -> !Boolean.TRUE.equals(r.getGold()))
            .toList();
        long accepts = reviews
            .stream()
            .filter(r -> r.getVerdict() == Verdict.ACCEPT)
            .count();
        long rejects = reviews.size() - accepts;
        if (accepts >= AGREEMENT) {
            recording.setStatus(RecordingStatus.VALIDATED);
        } else if (rejects >= AGREEMENT) {
            recording.setStatus(RecordingStatus.REJECTED);
        } else {
            return;
        }
        recordingRepository.save(recording);
        // Credits : l'auteur d'un enregistrement valide, et chaque validateur dont l'avis va dans le sens du consensus.
        // Le registre ignore un gain deja verse, donc un rejeu ne paie pas deux fois.
        Verdict outcome = recording.getStatus() == RecordingStatus.VALIDATED ? Verdict.ACCEPT : Verdict.REJECT;
        if (outcome == Verdict.ACCEPT) {
            creditService.awardRecordingValidated(recording);
        }
        reviews
            .stream()
            .filter(r -> r.getVerdict() == outcome)
            .forEach(r -> creditService.awardReviewAgreed(recording, r.getReviewer()));
        // Un enregistrement tranche peut conclure la periode d'essai de son auteur.
        trialService.evaluate(recording.getContributor());
        LOG.info("Enregistrement {} tranche : {} ({} pour, {} contre)", recording.getId(), recording.getStatus(), accepts, rejects);
    }

    /** Tranche a l'unanimite par au moins {@value #AGREEMENT} relectures : seuls ces enregistrements servent de pieges. */
    private boolean unanimous(Recording recording) {
        if (recording.getStatus() == RecordingStatus.SUBMITTED) {
            return false;
        }
        Verdict expected = recording.getStatus() == RecordingStatus.VALIDATED ? Verdict.ACCEPT : Verdict.REJECT;
        List<RecordingReview> reviews = reviewRepository
            .findAllByRecordingId(recording.getId())
            .stream()
            .filter(r -> !Boolean.TRUE.equals(r.getGold()))
            .toList();
        return reviews.size() >= AGREEMENT && reviews.stream().allMatch(r -> r.getVerdict() == expected);
    }

    /** Recalcule le score (part de bonnes reponses aux pieges, sur 100) et revoque si la fiabilite est trop basse. */
    private void updateReliability(UserProfile me, Language language) {
        Object[] row = reviewRepository.trapStats(me.getId(), language.getId()).get(0);
        long total = ((Number) row[0]).longValue();
        long correct = ((Number) row[1]).longValue();
        qualificationRepository.findOneByProfileIdAndLanguageId(me.getId(), language.getId()).ifPresent(q -> {
            double accuracy = total == 0 ? 1 : (double) correct / total;
            q.setScore((float) Math.round(accuracy * 1000) / 10f);
            if (total >= TRAP_MIN_SAMPLE && accuracy < TRAP_MIN_ACCURACY) {
                q.setStatus(QualificationStatus.REVOKED);
                LOG.warn("Qualification {} revoquee : {}/{} pieges reussis", q.getId(), correct, total);
            }
            qualificationRepository.save(q);
        });
    }

    private void requireQualified(UserProfile me, Language language) {
        boolean qualified = qualificationRepository
            .findOneByProfileIdAndLanguageId(me.getId(), language.getId())
            .filter(q -> q.getStatus() == QualificationStatus.QUALIFIED)
            .isPresent();
        if (!qualified) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Qualification requise pour cette langue");
        }
    }

    private Language languageOf(String code) {
        return languageRepository
            .findOneByCode(code)
            .orElseThrow(() -> new BadRequestAlertException("Langue introuvable", ENTITY_NAME, "languagenotfound"));
    }

    private static Language languageOfRecording(Recording recording) {
        return recording.getPrompt().getCampaign().getLanguage();
    }

    private static TaskView toTask(Recording r) {
        return new TaskView(r.getId(), r.getPrompt().getContent(), r.getDurationMs());
    }

    private static QualificationView toView(LanguageQualification q) {
        return new QualificationView(q.getId(), q.getLanguage().getCode(), q.getLanguage().getName(), q.getStatus(), q.getScore());
    }

    private static String contentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }
}
