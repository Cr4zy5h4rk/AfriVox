package com.afrivox.app.service;

import com.afrivox.app.domain.Campaign;
import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.afrivox.app.repository.CampaignRepository;
import com.afrivox.app.repository.PromptRepository;
import com.afrivox.app.repository.RecordingRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vue d'ensemble des campagnes pour l'administration : filtres cumulables et compteurs (phrases, enregistrements
 * par état) calculés en base, sans rien stocker en double.
 */
@Service
@Transactional(readOnly = true)
public class AdminCampaignOverviewService {

    public record CampaignRow(
        Long id,
        String title,
        String description,
        String domain,
        CampaignKind kind,
        CampaignStatus status,
        Integer targetSentences,
        Instant createdAt,
        Long languageId,
        String languageName,
        long prompts,
        long recordingsSubmitted,
        long recordingsValidated,
        long recordingsRejected,
        /** Tarifs propres a la campagne ({@code null} = tarif global). */
        Integer creditValidatedRecording,
        Integer creditReviewAgreed,
        Integer creditTrapCorrect
    ) {}

    private final CampaignRepository campaignRepository;
    private final PromptRepository promptRepository;
    private final RecordingRepository recordingRepository;

    public AdminCampaignOverviewService(
        CampaignRepository campaignRepository,
        PromptRepository promptRepository,
        RecordingRepository recordingRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.promptRepository = promptRepository;
        this.recordingRepository = recordingRepository;
    }

    /** Critères facultatifs : texte (titre), langue, type, état, domaine (égalité, sans tenir compte de la casse). */
    public Page<CampaignRow> search(
        String query,
        Long languageId,
        CampaignKind kind,
        CampaignStatus status,
        String domain,
        Pageable pageable
    ) {
        Specification<Campaign> spec = (root, q, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String like = "%" + escapeLike(query.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("title")), like, '\\'));
        }
        if (languageId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("language").get("id"), languageId));
        }
        if (kind != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("kind"), kind));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (domain != null && !domain.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("domain")), domain.trim().toLowerCase()));
        }
        Page<Campaign> page = campaignRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<Long> ids = page.getContent().stream().map(Campaign::getId).toList();
        Map<Long, Long> prompts = new HashMap<>();
        for (Object[] row : promptRepository.countByCampaign(ids)) {
            prompts.put((Long) row[0], (Long) row[1]);
        }
        // campagne -> (état -> nombre)
        Map<Long, Map<RecordingStatus, Long>> recordings = new HashMap<>();
        for (Object[] row : recordingRepository.countByCampaignAndStatus(ids)) {
            recordings.computeIfAbsent((Long) row[0], k -> new HashMap<>()).put((RecordingStatus) row[1], (Long) row[2]);
        }

        return page.map(c -> {
            Map<RecordingStatus, Long> byStatus = recordings.getOrDefault(c.getId(), Map.of());
            return new CampaignRow(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getDomain(),
                c.getKind(),
                c.getStatus(),
                c.getTargetSentences(),
                c.getCreatedAt(),
                c.getLanguage() == null ? null : c.getLanguage().getId(),
                c.getLanguage() == null ? null : c.getLanguage().getName(),
                prompts.getOrDefault(c.getId(), 0L),
                byStatus.getOrDefault(RecordingStatus.SUBMITTED, 0L),
                byStatus.getOrDefault(RecordingStatus.VALIDATED, 0L),
                byStatus.getOrDefault(RecordingStatus.REJECTED, 0L),
                c.getCreditValidatedRecording(),
                c.getCreditReviewAgreed(),
                c.getCreditTrapCorrect()
            );
        });
    }

    /** Domaines distincts, pour alimenter le filtre. */
    public List<String> domains() {
        return campaignRepository
            .findAll()
            .stream()
            .map(Campaign::getDomain)
            .filter(d -> d != null && !d.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
