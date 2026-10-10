package com.afrivox.app.service;

import com.afrivox.app.domain.DatasetRequest;
import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.afrivox.app.repository.DatasetRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Liste des demandes d'organisation pour l'administration, avec filtres cumulables et demandeur. */
@Service
@Transactional(readOnly = true)
public class AdminRequestOverviewService {

    public record LanguageRef(Long id, String code, String name) {}

    public record RequestRow(
        Long id,
        String orgName,
        Sector sector,
        Integer hours,
        Integer speakersTarget,
        String conversationType,
        UsageKind usageKind,
        LocalDate deadline,
        String description,
        RequestStatus status,
        Integer quoteAmountFcfa,
        Instant createdAt,
        LanguageRef language,
        String requesterLogin
    ) {}

    private final DatasetRequestRepository requestRepository;

    public AdminRequestOverviewService(DatasetRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /** Critères facultatifs : texte (organisation), état, secteur, langue, usage. */
    public Page<RequestRow> search(
        String query,
        RequestStatus status,
        Sector sector,
        Long languageId,
        UsageKind usageKind,
        Pageable pageable
    ) {
        Specification<DatasetRequest> spec = (root, q, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String like = "%" + escapeLike(query.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("orgName")), like, '\\'));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (sector != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("sector"), sector));
        }
        if (languageId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("language").get("id"), languageId));
        }
        if (usageKind != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("usageKind"), usageKind));
        }
        return requestRepository
            .findAll(spec, pageable)
            .map(r ->
                new RequestRow(
                    r.getId(),
                    r.getOrgName(),
                    r.getSector(),
                    r.getHours(),
                    r.getSpeakersTarget(),
                    r.getConversationType(),
                    r.getUsageKind(),
                    r.getDeadline(),
                    r.getDescription(),
                    r.getStatus(),
                    r.getQuoteAmountFcfa(),
                    r.getCreatedAt(),
                    r.getLanguage() == null
                        ? null
                        : new LanguageRef(r.getLanguage().getId(), r.getLanguage().getCode(), r.getLanguage().getName()),
                    r.getRequester() == null || r.getRequester().getUser() == null ? null : r.getRequester().getUser().getLogin()
                )
            );
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
