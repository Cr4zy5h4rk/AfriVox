package com.afrivox.app.service;

import com.afrivox.app.domain.CommentReport;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.ModelComment;
import com.afrivox.app.repository.CommentReportRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.ModelCommentRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * File de moderation (admin) : les commentaires signales et non traites. Deux issues : ignorer (les signalements sont
 * marques traites, le commentaire reste) ou supprimer le commentaire (voir {@code ModelCommentService.delete}, ses
 * signalements partent avec lui).
 */
@Service
@Transactional
public class CommentModerationService {

    public record ReportedComment(
        Long commentId,
        Long modelId,
        String modelName,
        String authorName,
        String body,
        Instant createdAt,
        int reports,
        List<String> reasons,
        List<String> details
    ) {}

    private final CommentReportRepository reportRepository;
    private final ModelCommentRepository commentRepository;
    private final MlModelRepository modelRepository;

    public CommentModerationService(
        CommentReportRepository reportRepository,
        ModelCommentRepository commentRepository,
        MlModelRepository modelRepository
    ) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.modelRepository = modelRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportedComment> open() {
        // Regroupe par commentaire en gardant l'ordre (le plus recemment signale d'abord).
        Map<Long, List<CommentReport>> byComment = new LinkedHashMap<>();
        for (CommentReport r : reportRepository.findOpen()) {
            byComment.computeIfAbsent(r.getCommentId(), id -> new ArrayList<>()).add(r);
        }
        if (byComment.isEmpty()) {
            return List.of();
        }
        Map<Long, ModelComment> comments = new HashMap<>();
        commentRepository.findAllById(byComment.keySet()).forEach(c -> comments.put(c.getId(), c));
        Set<Long> modelIds = new java.util.HashSet<>();
        comments.values().forEach(c -> modelIds.add(c.getModelId()));
        Map<Long, String> modelNames = new HashMap<>();
        for (MlModel m : modelRepository.findAllById(modelIds)) {
            modelNames.put(m.getId(), m.getName());
        }
        List<ReportedComment> result = new ArrayList<>();
        for (Map.Entry<Long, List<CommentReport>> e : byComment.entrySet()) {
            ModelComment c = comments.get(e.getKey());
            if (c == null) {
                continue;
            }
            List<CommentReport> reports = e.getValue();
            result.add(
                new ReportedComment(
                    c.getId(),
                    c.getModelId(),
                    modelNames.get(c.getModelId()),
                    c.getAuthor().getDisplayName(),
                    c.getBody(),
                    c.getCreatedAt(),
                    reports.size(),
                    reports.stream().map(CommentReport::getReason).distinct().toList(),
                    reports
                        .stream()
                        .map(CommentReport::getDetails)
                        .filter(d -> d != null && !d.isBlank())
                        .toList()
                )
            );
        }
        return result;
    }

    public long openCount() {
        return reportRepository.countByResolvedFalse();
    }

    /** Ignore les signalements d'un commentaire (il reste visible). */
    public void dismiss(Long commentId) {
        reportRepository.findAllByCommentIdInAndResolvedFalse(Set.of(commentId)).forEach(r -> r.setResolved(true));
    }
}
