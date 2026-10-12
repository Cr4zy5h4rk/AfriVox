package com.afrivox.app.service;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.Prompt;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.domain.enumeration.ScriptKind;
import com.afrivox.app.domain.enumeration.TextDirection;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.repository.PromptRepository;
import com.afrivox.app.repository.RecordingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ce que voit un contributeur : les campagnes ouvertes et, pour chacune, les phrases qu'il lui reste a lire.
 * Les compteurs sont calcules a la demande depuis la base (aucun chiffre stocke ni invente).
 */
@Service
// Pas readOnly : chaque methode passe par getOrCreateCurrent(), qui peut creer le profil (INSERT).
@Transactional
public class ContributionService {

    /** Nombre maximal de phrases renvoyees d'un coup. */
    public static final int MAX_BATCH = 50;

    /** Campagne vue par le contributeur, avec sa progression personnelle. */
    public record CampaignView(
        Long id,
        String title,
        String description,
        String domain,
        String languageCode,
        String languageName,
        ScriptKind script,
        TextDirection direction,
        long totalPrompts,
        long recordedByMe,
        /** Credits promis par enregistrement valide dans cette campagne (tarif de la campagne ou global). */
        int creditsPerRecording
    ) {}

    /** Phrase a lire. */
    public record PromptView(Long id, String content) {}

    private final CampaignRepository campaignRepository;

    private final PromptRepository promptRepository;

    private final RecordingRepository recordingRepository;

    private final UserProfileService userProfileService;

    private final CreditService creditService;

    public ContributionService(
        CampaignRepository campaignRepository,
        PromptRepository promptRepository,
        RecordingRepository recordingRepository,
        UserProfileService userProfileService,
        CreditService creditService
    ) {
        this.creditService = creditService;
        this.campaignRepository = campaignRepository;
        this.promptRepository = promptRepository;
        this.recordingRepository = recordingRepository;
        this.userProfileService = userProfileService;
    }

    /** Campagnes ouvertes, avec ma progression. */
    public List<CampaignView> openCampaigns() {
        UserProfile profile = userProfileService.getOrCreateCurrent();
        return campaignRepository
            .findAllOpenForContributors()
            .stream()
            .map(campaign -> toView(campaign, profile.getId()))
            .toList();
    }

    /**
     * Prochaines phrases a enregistrer dans une campagne.
     *
     * @return vide si la campagne n'existe pas, n'est pas active ou est privee (meme reponse : on ne revele rien).
     */
    public Optional<List<PromptView>> todo(Long campaignId, int limit) {
        Optional<Campaign> campaign = campaignRepository
            .findById(campaignId)
            .filter(c -> c.getStatus() == CampaignStatus.ACTIVE && c.getKind() != CampaignKind.PRIVATE);
        if (campaign.isEmpty()) {
            return Optional.empty();
        }
        UserProfile profile = userProfileService.getOrCreateCurrent();
        int size = Math.max(1, Math.min(limit, MAX_BATCH));
        List<Prompt> prompts = promptRepository.findTodo(campaignId, profile.getId(), PageRequest.of(0, size));
        return Optional.of(
            prompts
                .stream()
                .map(p -> new PromptView(p.getId(), p.getContent()))
                .toList()
        );
    }

    private CampaignView toView(Campaign campaign, Long profileId) {
        Language language = campaign.getLanguage();
        return new CampaignView(
            campaign.getId(),
            campaign.getTitle(),
            campaign.getDescription(),
            campaign.getDomain(),
            language == null ? null : language.getCode(),
            language == null ? null : language.getName(),
            language == null ? null : language.getScript(),
            language == null ? null : language.getDirection(),
            promptRepository.countByCampaignId(campaign.getId()),
            recordingRepository.countRecordedPrompts(profileId, campaign.getId()),
            creditService.rewardForNewRecording(campaign)
        );
    }
}
