package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.CreditRateKind;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Journal des changements de tarifs (globaux ou propres a une campagne) : qui a change quoi, quand, de combien a
 * combien. Jamais modifie ni supprime.
 */
@Entity
@Table(name = "credit_rate_history")
public class CreditRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /** {@code GLOBAL} ou {@code CAMPAIGN:<id>}. */
    @Column(name = "scope", length = 80, nullable = false)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30, nullable = false)
    private CreditRateKind kind;

    /** {@code null} : pas de tarif propre a la campagne (tarif global). */
    @Column(name = "old_amount")
    private Integer oldAmount;

    @Column(name = "new_amount")
    private Integer newAmount;

    @Column(name = "changed_by", length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected CreditRateHistory() {}

    public CreditRateHistory(String scope, CreditRateKind kind, Integer oldAmount, Integer newAmount, String changedBy, Instant changedAt) {
        this.scope = scope;
        this.kind = kind;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public String getScope() {
        return scope;
    }

    public CreditRateKind getKind() {
        return kind;
    }

    public Integer getOldAmount() {
        return oldAmount;
    }

    public Integer getNewAmount() {
        return newAmount;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
