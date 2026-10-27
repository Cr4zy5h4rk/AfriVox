package com.afrivox.app.service;

import com.afrivox.app.domain.CommentReport;
import com.afrivox.app.domain.MlModel;
import com.afrivox.app.domain.ModelComment;
import com.afrivox.app.domain.Project;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.CommentReportRepository;
import com.afrivox.app.repository.MlModelRepository;
import com.afrivox.app.repository.ModelCommentRepository;
import com.afrivox.app.repository.ProjectRepository;
import com.afrivox.app.security.AuthoritiesConstants;
import com.afrivox.app.security.SecurityUtils;
import com.afrivox.app.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Section communaute d'un modele ou d'un projet : commentaires lisibles sans compte, ecrits par tout compte connecte, avec un seul
 * niveau de reponses. Le texte est stocke tel quel et affiche comme du texte par le front (jamais comme du HTML).
 * <p>
 * Moderation : l'auteur supprime les siens (et les reponses partent avec le commentaire), l'admin supprime n'importe
 * lequel ; tout utilisateur peut signaler un commentaire, l'admin traite la file ({@link CommentModerationService}).
 * Notifications : le proprietaire du modele (ou du projet) est prevenu d'un commentaire, l'auteur d'un commentaire d'une reponse.
 */
@Service
@Transactional
public class ModelCommentService {

    private static final String ENTITY_NAME = "modelComment";

    static final int MAX_LENGTH = 1000;
    static final int MAX_PAGE_SIZE = 50;
    /** Au plus {@value} commentaires (ou reponses) par auteur et par minute (contre le spam). */
    static final int MAX_PER_MINUTE = 5;
    static final Set<String> REPORT_REASONS = Set.of("SPAM", "ABUSE", "OFF_TOPIC", "OTHER");

    public record CommentView(
        Long id,
        Long parentId,
        String authorName,
        String body,
        Instant createdAt,
        boolean mine,
        boolean canDelete,
        List<CommentView> replies
    ) {}

    public record CommentPage(List<CommentView> items, int page, int size, long total, int pages, long totalWithReplies) {}

    private final ModelCommentRepository commentRepository;
    private final CommentReportRepository reportRepository;
    private final MlModelRepository modelRepository;
    private final ProjectRepository projectRepository;
    private final UserProfileService userProfileService;
    private final NotificationService notifications;

    public ModelCommentService(
        ModelCommentRepository commentRepository,
        CommentReportRepository reportRepository,
        MlModelRepository modelRepository,
        ProjectRepository projectRepository,
        UserProfileService userProfileService,
        NotificationService notifications
    ) {
        this.commentRepository = commentRepository;
        this.reportRepository = reportRepository;
        this.modelRepository = modelRepository;
        this.projectRepository = projectRepository;
        this.userProfileService = userProfileService;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public CommentPage list(Long modelId, int page, int size) {
        if (!modelRepository.existsById(modelId)) {
            throw new BadRequestAlertException("Modele introuvable", ENTITY_NAME, "modelnotfound");
        }
        Page<ModelComment> result = commentRepository.findForModel(modelId, pageRequest(page, size));
        return toPage(result, commentRepository.countByModelId(modelId));
    }

    @Transactional(readOnly = true)
    public CommentPage listForProject(Long projectId, int page, int size) {
        if (!projectRepository.existsById(projectId)) {
            throw new BadRequestAlertException("Projet introuvable", ENTITY_NAME, "projectnotfound");
        }
        Page<ModelComment> result = commentRepository.findForProject(projectId, pageRequest(page, size));
        return toPage(result, commentRepository.countByProjectId(projectId));
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    private CommentPage toPage(Page<ModelComment> result, long totalWithReplies) {
        // Les reponses des commentaires de la page, en une seule requete.
        List<Long> ids = result.getContent().stream().map(ModelComment::getId).toList();
        Map<Long, List<ModelComment>> repliesByParent = ids.isEmpty()
            ? Map.of()
            : commentRepository.findReplies(ids).stream().collect(Collectors.groupingBy(ModelComment::getParentId));
        String login = SecurityUtils.getCurrentUserLogin().orElse(null);
        boolean admin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        List<CommentView> items = new ArrayList<>();
        for (ModelComment c : result.getContent()) {
            List<CommentView> replies = repliesByParent
                .getOrDefault(c.getId(), List.of())
                .stream()
                .map(r -> view(r, login, admin, List.of()))
                .toList();
            items.add(view(c, login, admin, replies));
        }
        return new CommentPage(
            items,
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            totalWithReplies
        );
    }

    /** Pas de {@code readOnly} : le profil de l'auteur peut etre cree au premier usage. */
    public CommentView add(Long modelId, String body, Long parentId) {
        MlModel model = modelRepository
            .findById(modelId)
            .orElseThrow(() -> new BadRequestAlertException("Modele introuvable", ENTITY_NAME, "modelnotfound"));
        return post(new Target(modelId, null, model.getName(), model.getOwner()), body, parentId);
    }

    public CommentView addToProject(Long projectId, String body, Long parentId) {
        Project project = projectRepository
            .findWithOwner(projectId)
            .orElseThrow(() -> new BadRequestAlertException("Projet introuvable", ENTITY_NAME, "projectnotfound"));
        return post(new Target(null, projectId, project.getTitle(), project.getOwner()), body, parentId);
    }

    /** Cible d'un commentaire : un modele ou un projet, avec son nom et son proprietaire (a prevenir). */
    private record Target(Long modelId, Long projectId, String name, UserProfile owner) {
        boolean sameAs(ModelComment c) {
            return modelId != null ? modelId.equals(c.getModelId()) : projectId.equals(c.getProjectId());
        }
    }

    private CommentView post(Target target, String body, Long parentId) {
        String text = body == null ? "" : body.strip();
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new BadRequestAlertException("Le commentaire doit faire entre 1 et 1000 caracteres", ENTITY_NAME, "invalidcomment");
        }
        ModelComment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId).orElse(null);
            // Une reponse se rattache a un commentaire de premier niveau de la meme cible (pas de fil de fil).
            if (parent == null || !target.sameAs(parent) || parent.getParentId() != null) {
                throw new BadRequestAlertException("Commentaire parent invalide", ENTITY_NAME, "invalidparent");
            }
        }
        UserProfile author = userProfileService.getOrCreateCurrent();
        if (
            commentRepository.countByAuthorIdAndCreatedAtAfter(author.getId(), Instant.now().minus(Duration.ofMinutes(1))) >= MAX_PER_MINUTE
        ) {
            throw new BadRequestAlertException("Trop de commentaires, reessayez dans une minute", ENTITY_NAME, "toomanycomments");
        }
        ModelComment saved = commentRepository.save(
            new ModelComment(target.modelId(), target.projectId(), author, text, Instant.now(), parentId)
        );

        // Notifications : l'auteur du commentaire parent, puis le proprietaire de la cible (sauf si c'est la meme
        // personne ou l'auteur lui-meme).
        boolean onProject = target.projectId() != null;
        UserProfile owner = target.owner();
        UserProfile parentAuthor = parent == null ? null : parent.getAuthor();
        if (parentAuthor != null && !parentAuthor.getId().equals(author.getId())) {
            notifyTarget(parentAuthor, onProject ? NotificationService.PROJECT_REPLY : NotificationService.COMMENT_REPLY, author, target);
        }
        if (
            owner != null && !owner.getId().equals(author.getId()) && (parentAuthor == null || !parentAuthor.getId().equals(owner.getId()))
        ) {
            notifyTarget(owner, onProject ? NotificationService.PROJECT_COMMENT : NotificationService.MODEL_COMMENT, author, target);
        }
        return new CommentView(
            saved.getId(),
            parentId,
            author.getDisplayName(),
            saved.getBody(),
            saved.getCreatedAt(),
            true,
            true,
            List.of()
        );
    }

    private void notifyTarget(UserProfile recipient, String type, UserProfile author, Target target) {
        if (target.projectId() != null) {
            notifications.notifyProject(recipient, type, author.getDisplayName(), target.projectId(), target.name());
        } else {
            notifications.notify(recipient, type, author.getDisplayName(), target.modelId(), target.name());
        }
    }

    /** Supprime un commentaire (ses reponses partent avec lui) ; faux s'il n'existe pas ou n'est pas a l'utilisateur. */
    public boolean delete(Long commentId) {
        UserProfile me = userProfileService.getOrCreateCurrent();
        boolean admin = SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN);
        return commentRepository
            .findById(commentId)
            .filter(c -> admin || c.getAuthor().getId().equals(me.getId()))
            .map(c -> {
                commentRepository.delete(c);
                return true;
            })
            .orElse(false);
    }

    /** Signale un commentaire a la moderation (un signalement par personne et par commentaire, jamais le sien). */
    public void report(Long commentId, String reason, String details) {
        if (reason == null || !REPORT_REASONS.contains(reason)) {
            throw new BadRequestAlertException("Motif invalide", ENTITY_NAME, "invalidreason");
        }
        UserProfile me = userProfileService.getOrCreateCurrent();
        ModelComment comment = commentRepository
            .findById(commentId)
            .orElseThrow(() -> new BadRequestAlertException("Commentaire introuvable", ENTITY_NAME, "commentnotfound"));
        if (comment.getAuthor().getId().equals(me.getId())) {
            throw new BadRequestAlertException("On ne signale pas son propre commentaire", ENTITY_NAME, "ownreport");
        }
        if (reportRepository.existsByCommentIdAndReporterId(commentId, me.getId())) {
            throw new BadRequestAlertException("Deja signale", ENTITY_NAME, "alreadyreported");
        }
        String cleaned = details == null || details.isBlank() ? null : details.strip();
        if (cleaned != null && cleaned.length() > 300) {
            cleaned = cleaned.substring(0, 300);
        }
        reportRepository.save(new CommentReport(commentId, me, reason, cleaned, Instant.now()));
    }

    private static CommentView view(ModelComment c, String currentLogin, boolean admin, List<CommentView> replies) {
        UserProfile author = c.getAuthor();
        boolean mine = currentLogin != null && author.getUser() != null && currentLogin.equals(author.getUser().getLogin());
        return new CommentView(
            c.getId(),
            c.getParentId(),
            author.getDisplayName(),
            c.getBody(),
            c.getCreatedAt(),
            mine,
            mine || admin,
            replies
        );
    }
}
