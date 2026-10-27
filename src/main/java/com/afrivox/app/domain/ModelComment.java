package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Commentaire d'un utilisateur sur la fiche d'un modele ou d'un projet (section communaute) : exactement l'un des deux
 * est renseigne (contrainte en base). Le nom de la classe date de l'epoque ou seuls les modeles se commentaient.
 */
@Entity
@Table(name = "model_comment")
public class ModelComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /** Modele commente, ou {@code null} pour un commentaire de projet. */
    @Column(name = "model_id")
    private Long modelId;

    /** Projet commente, ou {@code null} pour un commentaire de modele. */
    @Column(name = "project_id")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserProfile author;

    @NotNull
    @Size(min = 1, max = 1000)
    @Column(name = "body", length = 1000, nullable = false)
    private String body;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Commentaire auquel celui-ci repond ({@code null} = premier niveau ; un seul niveau de reponses). */
    @Column(name = "parent_id")
    private Long parentId;

    protected ModelComment() {}

    public ModelComment(Long modelId, Long projectId, UserProfile author, String body, Instant createdAt, Long parentId) {
        this.parentId = parentId;
        this.modelId = modelId;
        this.projectId = projectId;
        this.author = author;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getModelId() {
        return modelId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public UserProfile getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
