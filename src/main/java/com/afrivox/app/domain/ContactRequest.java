package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.ContactStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Demande de contact d'une organisation vers un developpeur ouvert aux opportunites. Le developpeur lit le message et
 * le moyen de joindre l'organisation ({@code contact}) et decide ; son propre contact n'est jamais communique.
 */
@Entity
@Table(name = "contact_request")
public class ContactRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserProfile sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserProfile recipient;

    @Column(name = "message", length = 1000, nullable = false)
    private String message;

    /** Adresse e-mail, lien ou numero que l'expediteur donne pour etre rappele. */
    @Column(name = "contact", length = 120, nullable = false)
    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private ContactStatus status = ContactStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected ContactRequest() {}

    public ContactRequest(UserProfile sender, UserProfile recipient, String message, String contact, Instant createdAt) {
        this.sender = sender;
        this.recipient = recipient;
        this.message = message;
        this.contact = contact;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getSender() {
        return sender;
    }

    public UserProfile getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public String getContact() {
        return contact;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void answer(ContactStatus status, Instant at) {
        this.status = status;
        this.answeredAt = at;
    }
}
