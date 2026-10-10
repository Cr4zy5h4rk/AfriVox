package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Commentaire d'un utilisateur sur la fiche d'un modele (section communaute). */
@Entity
@Table(name = "model_comment")
public class ModelComment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "model_id", nullable = false)
    private Long modelId;

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

    public ModelComment(Long modelId, UserProfile author, String body, Instant createdAt, Long parentId) {
        this.parentId = parentId;
        this.modelId = modelId;
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
