package com.afrivox.app.service;

import com.afrivox.app.domain.Notification;
import com.afrivox.app.domain.UserProfile;
import com.afrivox.app.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications de la communaute (commentaire sur mon modele, reponse a mon commentaire). Pas de temps reel (pas de
 * WebSocket dans le projet) : l'interface relit le compteur a la navigation. Chacun ne voit que les siennes.
 */
@Service
@Transactional
public class NotificationService {

    public static final String MODEL_COMMENT = "MODEL_COMMENT";
    public static final String COMMENT_REPLY = "COMMENT_REPLY";
    public static final String PROJECT_COMMENT = "PROJECT_COMMENT";
    public static final String PROJECT_REPLY = "PROJECT_REPLY";
    public static final String CONTACT_REQUEST = "CONTACT_REQUEST";
    public static final String CONTACT_ACCEPTED = "CONTACT_ACCEPTED";

    static final int MAX_LISTED = 30;

    public record NotificationView(
        Long id,
        String type,
        String actorName,
        Long modelId,
        String modelName,
        Long projectId,
        String projectName,
        Instant createdAt,
        boolean read
    ) {}

    public record NotificationList(List<NotificationView> items, long unread) {}

    private final NotificationRepository repository;
    private final UserProfileService userProfileService;

    public NotificationService(NotificationRepository repository, UserProfileService userProfileService) {
        this.repository = repository;
        this.userProfileService = userProfileService;
    }

    /** Cree une notification (jamais pour soi-meme : l'appelant filtre). */
    void notify(UserProfile recipient, String type, String actorName, Long modelId, String modelName) {
        String actor = actorName != null && actorName.length() > 80 ? actorName.substring(0, 80) : actorName;
        String model = modelName != null && modelName.length() > 120 ? modelName.substring(0, 120) : modelName;
        repository.save(new Notification(recipient, type, actor, modelId, model, null, null, Instant.now()));
    }

    /** Notification liee a un projet (commentaire, reponse). */
    void notifyProject(UserProfile recipient, String type, String actorName, Long projectId, String projectName) {
        String actor = actorName != null && actorName.length() > 80 ? actorName.substring(0, 80) : actorName;
        String project = projectName != null && projectName.length() > 120 ? projectName.substring(0, 120) : projectName;
        repository.save(new Notification(recipient, type, actor, null, null, projectId, project, Instant.now()));
    }

    /** Notification sans cible (demande de contact) : seul l'auteur est indique, la page de destination est fixe. */
    void notifyActor(UserProfile recipient, String type, String actorName) {
        String actor = actorName != null && actorName.length() > 80 ? actorName.substring(0, 80) : actorName;
        repository.save(new Notification(recipient, type, actor, null, null, null, null, Instant.now()));
    }

    /** Pas de {@code readOnly} : le profil peut etre cree au premier usage. */
    public NotificationList mine() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        List<NotificationView> items = repository
            .findByRecipientIdOrderByCreatedAtDesc(me.getId(), PageRequest.of(0, MAX_LISTED))
            .stream()
            .map(n ->
                new NotificationView(
                    n.getId(),
                    n.getType(),
                    n.getActorName(),
                    n.getModelId(),
                    n.getModelName(),
                    n.getProjectId(),
                    n.getProjectName(),
                    n.getCreatedAt(),
                    n.getReadAt() != null
                )
            )
            .toList();
        return new NotificationList(items, repository.countByRecipientIdAndReadAtIsNull(me.getId()));
    }

    public long unreadCount() {
        return repository.countByRecipientIdAndReadAtIsNull(userProfileService.getOrCreateCurrent().getId());
    }

    public void markAllRead() {
        UserProfile me = userProfileService.getOrCreateCurrent();
        Instant now = Instant.now();
        repository.findByRecipientIdAndReadAtIsNull(me.getId()).forEach(n -> n.setReadAt(now));
    }
}
