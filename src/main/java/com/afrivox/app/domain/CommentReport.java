package com.afrivox.app.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Signalement d'un commentaire par un utilisateur (un seul par couple commentaire/signaleur). */
@Entity
@Table(name = "comment_report")
public class CommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserProfile reporter;

    @Column(name = "reason", length = 20, nullable = false)
    private String reason;

    @Column(name = "details", length = 300)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    protected CommentReport() {}

    public CommentReport(Long commentId, UserProfile reporter, String reason, String details, Instant createdAt) {
        this.commentId = commentId;
        this.reporter = reporter;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public UserProfile getReporter() {
        return reporter;
    }

    public String getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
