package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Projet publie par un developpeur dans le hub : un lien vers son depot et/ou une demonstration, sans heberger de code
 * ni de fichier. Les etiquettes sont stockees normalisees, separees par des virgules.
 */
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserProfile owner;

    @NotNull
    @Size(max = 120)
    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @NotNull
    @Size(max = 1000)
    @Column(name = "summary", length = 1000, nullable = false)
    private String summary;

    @Column(name = "repo_url", length = 300)
    private String repoUrl;

    @Column(name = "demo_url", length = 300)
    private String demoUrl;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    /** Etiquettes normalisees (minuscules), separees par des virgules ; {@code null} s'il n'y en a pas. */
    @Column(name = "tags", length = 200)
    private String tags;

    /** Modele du Model Hub lie au projet (facultatif). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private MlModel model;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public UserProfile getOwner() {
        return owner;
    }

    public void setOwner(UserProfile owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public MlModel getModel() {
        return model;
    }

    public void setModel(MlModel model) {
        this.model = model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
