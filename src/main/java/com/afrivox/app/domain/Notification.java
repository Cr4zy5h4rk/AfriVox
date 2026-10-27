package com.afrivox.app.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Notification d'un utilisateur. Seuls le type et des reperes (auteur, modele) sont stockes : le texte est compose par
 * l'interface dans la langue de l'utilisateur.
 */
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserProfile recipient;

    @Column(name = "type", length = 30, nullable = false)
    private String type;

    @Column(name = "actor_name", length = 80)
    private String actorName;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_name", length = 120)
    private String projectName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {}

    public Notification(
        UserProfile recipient,
        String type,
        String actorName,
        Long modelId,
        String modelName,
        Long projectId,
        String projectName,
        Instant createdAt
    ) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.recipient = recipient;
        this.type = type;
        this.actorName = actorName;
        this.modelId = modelId;
        this.modelName = modelName;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getRecipient() {
        return recipient;
    }

    public String getType() {
        return type;
    }

    public String getActorName() {
        return actorName;
    }

    public Long getModelId() {
        return modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
