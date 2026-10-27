package com.afrivox.app.web.rest;

import com.afrivox.app.service.CommentModerationService;
import com.afrivox.app.service.CommentModerationService.ReportedComment;
import com.afrivox.app.service.ModelCommentService;
import com.afrivox.app.service.ModelCommentService.CommentPage;
import com.afrivox.app.service.ModelCommentService.CommentView;
import com.afrivox.app.service.NotificationService;
import com.afrivox.app.service.NotificationService.NotificationList;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Communaute autour des modeles. Lecture publique ({@code /api/public/**}) ; ecrire, repondre, signaler, supprimer et
 * notifications exigent un compte connecte, quel que soit son role (regle par defaut {@code /api/**}). La file de
 * moderation est sous {@code /api/admin/**} (admin seulement).
 */
@RestController
@RequestMapping("/api")
public class ModelCommentResource {

    public record CommentRequest(@NotBlank @Size(max = 1000) String body, Long parentId) {}

    public record ReportRequest(@NotBlank @Size(max = 20) String reason, @Size(max = 300) String details) {}

    private final ModelCommentService service;
    private final NotificationService notifications;
    private final CommentModerationService moderation;

    public ModelCommentResource(ModelCommentService service, NotificationService notifications, CommentModerationService moderation) {
        this.service = service;
        this.notifications = notifications;
        this.moderation = moderation;
    }

    @GetMapping("/public/models/{modelId}/comments")
    public CommentPage list(
        @PathVariable Long modelId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(modelId, page, size);
    }

    @PostMapping("/community/models/{modelId}/comments")
    public ResponseEntity<CommentView> add(@PathVariable Long modelId, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(modelId, request.body(), request.parentId()));
    }

    // ---- Memes commentaires sur les projets du hub ----

    @GetMapping("/public/projects/{projectId}/comments")
    public CommentPage listForProject(
        @PathVariable Long projectId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return service.listForProject(projectId, page, size);
    }

    @PostMapping("/community/projects/{projectId}/comments")
    public ResponseEntity<CommentView> addToProject(@PathVariable Long projectId, @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addToProject(projectId, request.body(), request.parentId()));
    }

    @DeleteMapping("/community/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId) {
        return service.delete(commentId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/community/comments/{commentId}/report")
    public ResponseEntity<Void> report(@PathVariable Long commentId, @Valid @RequestBody ReportRequest request) {
        service.report(commentId, request.reason(), request.details());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ---- Notifications de l'utilisateur connecte ----

    @GetMapping("/community/notifications")
    public NotificationList notificationsList() {
        return notifications.mine();
    }

    @GetMapping("/community/notifications/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notifications.unreadCount());
    }

    @PostMapping("/community/notifications/read-all")
    public ResponseEntity<Void> readAll() {
        notifications.markAllRead();
        return ResponseEntity.noContent().build();
    }

    // ---- Moderation (admin) ----

    @GetMapping("/admin/comment-reports")
    public List<ReportedComment> reports() {
        return moderation.open();
    }

    @PostMapping("/admin/comment-reports/{commentId}/dismiss")
    public ResponseEntity<Void> dismiss(@PathVariable Long commentId) {
        moderation.dismiss(commentId);
        return ResponseEntity.noContent().build();
    }
}
