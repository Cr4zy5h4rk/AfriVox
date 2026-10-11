package com.afrivox.app.service;

import com.afrivox.app.config.ApplicationProperties;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.ContributorStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.repository.RecordingRepository;
import com.afrivox.app.repository.UserProfileRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periode d'essai des contributeurs.
 *
 * <ul>
 *   <li>Un nouveau contributeur est {@code TRIAL} : il peut envoyer au plus {@code maxRecordings} enregistrements
 *       (relus, rejetes ou en attente, tous comptent : un rejet ne libere pas de place).</li>
 *   <li>Ses enregistrements sont relus par les validateurs qualifies, comme les autres : aucune file speciale.</li>
 *   <li>Quand tous sont tranches, il passe {@code ACTIVE} tout seul si au moins {@code minAcceptRate} sont valides.
 *       Sinon il reste en essai et c'est l'admin qui decide (promouvoir ou suspendre).</li>
 * </ul>
 * L'admin peut aussi changer l'etat a tout moment. {@code SUSPENDED} bloque tout nouvel envoi.
 */
@Service
// Pas readOnly : mine() passe par getOrCreateCurrent(), qui peut creer le profil (INSERT).
@Transactional
public class ContributorTrialService {

    private static final Logger LOG = LoggerFactory.getLogger(ContributorTrialService.class);

    private static final String ENTITY_NAME = "contributor";

    /** Situation d'un contributeur, calculee depuis ses enregistrements (rien n'est stocke en double). */
    public record TrialView(ContributorStatus status, int limit, long used, long pending, long validated, long rejected) {}

    /** Ligne de la liste admin. */
    public record ContributorRow(
        Long id,
        String displayName,
        String login,
        ContributorStatus status,
        Instant createdAt,
        long used,
        long pending,
        long validated,
        long rejected
    ) {}

    private final ApplicationProperties.ContributorTrial properties;
    private final RecordingRepository recordingRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    public ContributorTrialService(
        ApplicationProperties applicationProperties,
        RecordingRepository recordingRepository,
        UserProfileRepository userProfileRepository,
        UserProfileService userProfileService
    ) {
        this.properties = applicationProperties.getContributorTrial();
        this.recordingRepository = recordingRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileService = userProfileService;
    }

    /** Ma situation (pour la banniere de l'ecran d'enregistrement). */
    public TrialView mine() {
        return view(userProfileService.getOrCreateCurrent());
    }

    /** Refuse l'envoi si le contributeur est suspendu ou a atteint le plafond de l'essai. */
    public void assertCanUpload(UserProfile profile) {
        ContributorStatus status = profile.getContributorStatus();
        if (status == ContributorStatus.SUSPENDED) {
            throw new BadRequestAlertException("Compte contributeur suspendu", ENTITY_NAME, "contributorsuspended");
        }
        if (status == ContributorStatus.TRIAL) {
            TrialView view = view(profile);
            if (view.used() >= properties.getMaxRecordings()) {
                throw new BadRequestAlertException("Limite de la periode d'essai atteinte", ENTITY_NAME, "triallimit");
            }
        }
    }

    /**
     * A appeler quand un enregistrement vient d'etre tranche : promeut le contributeur si son essai est concluant.
     * Ne retrograde jamais (un rejet massif se traite par l'admin).
     */
    public void evaluate(UserProfile profile) {
        if (profile.getContributorStatus() != ContributorStatus.TRIAL) {
            return;
        }
        TrialView view = view(profile);
        long settled = view.validated() + view.rejected();
        if (settled < properties.getMaxRecordings()) {
            return;
        }
        double rate = (double) view.validated() / settled;
        if (rate >= properties.getMinAcceptRate()) {
            profile.setContributorStatus(ContributorStatus.ACTIVE);
            userProfileRepository.save(profile);
            LOG.info("Contributeur {} promu : {}/{} enregistrements valides", profile.getId(), view.validated(), settled);
        }
    }

    // ---- Administration ------------------------------------------------------------------------------------------

    public Page<ContributorRow> list(ContributorStatus status, Pageable pageable) {
        return userProfileRepository.findContributors(status, pageable).map(p -> {
            TrialView v = view(p);
            return new ContributorRow(
                p.getId(),
                p.getDisplayName(),
                p.getUser() == null ? null : p.getUser().getLogin(),
                p.getContributorStatus(),
                p.getCreatedAt(),
                v.used(),
                v.pending(),
                v.validated(),
                v.rejected()
            );
        });
    }

    /** Decision de l'admin : promouvoir (ACTIVE), suspendre (SUSPENDED) ou remettre en essai (TRIAL). */
    public void setStatus(Long profileId, ContributorStatus status) {
        UserProfile profile = userProfileRepository
            .findById(profileId)
            .orElseThrow(() -> new BadRequestAlertException("Contributeur introuvable", ENTITY_NAME, "notfound"));
        profile.setContributorStatus(status);
        userProfileRepository.save(profile);
        LOG.info("Contributeur {} passe en {}", profileId, status);
    }

    // ---- Interne -------------------------------------------------------------------------------------------------

    private TrialView view(UserProfile profile) {
        Map<RecordingStatus, Long> counts = new EnumMap<>(RecordingStatus.class);
        for (Object[] row : recordingRepository.countByContributorAndStatus(profile.getId())) {
            counts.put((RecordingStatus) row[0], ((Number) row[1]).longValue());
        }
        long pending = counts.getOrDefault(RecordingStatus.SUBMITTED, 0L);
        long validated = counts.getOrDefault(RecordingStatus.VALIDATED, 0L);
        long rejected = counts.getOrDefault(RecordingStatus.REJECTED, 0L);
        return new TrialView(
            profile.getContributorStatus(),
            properties.getMaxRecordings(),
            pending + validated + rejected,
            pending,
            validated,
            rejected
        );
    }
}
