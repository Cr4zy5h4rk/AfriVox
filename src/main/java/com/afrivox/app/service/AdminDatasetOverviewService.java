package com.afrivox.app.service;

import com.afrivox.app.domain.Dataset;
import com.afrivox.app.domain.DatasetVersion;
import com.afrivox.app.domain.enumeration.DatasetPlan;
import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import com.afrivox.app.repository.DatasetRepository;
import com.afrivox.app.repository.DatasetVersionRepository;
import com.afrivox.app.repository.PurchaseRepository;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vue d'ensemble des datasets pour l'administration : chaque dataset avec ses versions et leurs compteurs
 * (enregistrements figés, achats). Les chiffres viennent de la base, rien n'est stocké en double.
 */
@Service
@Transactional(readOnly = true)
public class AdminDatasetOverviewService {

    public record VersionRow(
        Long id,
        String label,
        LicenseKind license,
        VersionStatus status,
        Float hours,
        Integer speakers,
        long recordings,
        Integer pricePerHourFcfa,
        long purchases,
        Instant createdAt
    ) {}

    public record DatasetRow(
        Long id,
        String slug,
        String title,
        String description,
        String domain,
        DatasetPlan plan,
        Long languageId,
        String languageName,
        List<VersionRow> versions
    ) {}

    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final PurchaseRepository purchaseRepository;

    public AdminDatasetOverviewService(
        DatasetRepository datasetRepository,
        DatasetVersionRepository versionRepository,
        PurchaseRepository purchaseRepository
    ) {
        this.datasetRepository = datasetRepository;
        this.versionRepository = versionRepository;
        this.purchaseRepository = purchaseRepository;
    }

    /**
     * Tous les critères sont facultatifs et se cumulent : texte (titre ou slug), langue, formule, domaine,
     * et état de publication ({@code published} vrai = au moins une version publiée, faux = aucune).
     */
    public Page<DatasetRow> search(String query, Long languageId, DatasetPlan plan, String domain, Boolean published, Pageable pageable) {
        Specification<Dataset> spec = (root, q, cb) -> cb.conjunction();
        if (query != null && !query.isBlank()) {
            String like = "%" + escapeLike(query.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) ->
                cb.or(cb.like(cb.lower(root.get("title")), like, '\\'), cb.like(cb.lower(root.get("slug")), like, '\\'))
            );
        }
        if (languageId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("language").get("id"), languageId));
        }
        if (plan != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("plan"), plan));
        }
        if (domain != null && !domain.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("domain")), domain.trim().toLowerCase()));
        }
        if (published != null) {
            spec = spec.and((root, q, cb) -> {
                var sub = q.subquery(Long.class);
                var v = sub.from(DatasetVersion.class);
                sub.select(v.get("id")).where(cb.equal(v.get("dataset"), root), cb.equal(v.get("status"), VersionStatus.PUBLISHED));
                return published ? cb.exists(sub) : cb.not(cb.exists(sub));
            });
        }
        Page<Dataset> page = datasetRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<Long> datasetIds = page.getContent().stream().map(Dataset::getId).toList();
        List<DatasetVersion> versions = versionRepository.findAllByDatasetIdInOrderByCreatedAtDesc(datasetIds);
        List<Long> versionIds = versions.stream().map(DatasetVersion::getId).toList();
        Map<Long, Long> recordings = counts(versionIds.isEmpty() ? List.of() : versionRepository.countRecordingsByVersion(versionIds));
        Map<Long, Long> purchases = counts(versionIds.isEmpty() ? List.of() : purchaseRepository.countByVersion(versionIds));

        Map<Long, List<VersionRow>> rowsByDataset = new HashMap<>();
        for (DatasetVersion v : versions) {
            rowsByDataset
                .computeIfAbsent(v.getDataset().getId(), k -> new java.util.ArrayList<>())
                .add(
                    new VersionRow(
                        v.getId(),
                        v.getLabel(),
                        v.getLicense(),
                        v.getStatus(),
                        v.getHours(),
                        v.getSpeakers(),
                        recordings.getOrDefault(v.getId(), 0L),
                        v.getPricePerHourFcfa(),
                        purchases.getOrDefault(v.getId(), 0L),
                        v.getCreatedAt()
                    )
                );
        }
        return page.map(d ->
            new DatasetRow(
                d.getId(),
                d.getSlug(),
                d.getTitle(),
                d.getDescription(),
                d.getDomain(),
                d.getPlan(),
                d.getLanguage() == null ? null : d.getLanguage().getId(),
                d.getLanguage() == null ? null : d.getLanguage().getName(),
                rowsByDataset.getOrDefault(d.getId(), List.of())
            )
        );
    }

    /** Domaines distincts, pour alimenter le filtre. */
    public List<String> domains() {
        return datasetRepository
            .findAll()
            .stream()
            .map(Dataset::getDomain)
            .filter(d -> d != null && !d.isBlank())
            .distinct()
            .sorted()
            .toList();
    }

    private static Map<Long, Long> counts(Collection<Object[]> rows) {
        Map<Long, Long> out = new HashMap<>();
        for (Object[] row : rows) {
            out.put((Long) row[0], (Long) row[1]);
        }
        return out;
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Retire ou remet en ligne une version déjà publiée ; une version sans archive ne peut pas être remise en ligne. */
    @Transactional
    public Optional<DatasetVersion> setVersionStatus(Long versionId, VersionStatus status) {
        return versionRepository.findById(versionId).map(v -> {
            if (status == VersionStatus.PUBLISHED && v.getPackagePath() == null) {
                throw new BadRequestAlertException("Cette version n'a pas d'archive", "datasetVersion", "nopackage");
            }
            v.setStatus(status);
            return v;
        });
    }
}
