package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.CreditRateKind;
import jakarta.persistence.*;
import java.time.Instant;

/** Tarif global d'un type de gain, modifiable par l'admin (une ligne par {@link CreditRateKind}). */
@Entity
@Table(name = "credit_rate")
public class CreditRate {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", length = 30, nullable = false)
    private CreditRateKind kind;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    protected CreditRate() {}

    public CreditRate(CreditRateKind kind, int amount, Instant updatedAt, String updatedBy) {
        this.kind = kind;
        this.amount = amount;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public CreditRateKind getKind() {
        return kind;
    }

    public int getAmount() {
        return amount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void change(int amount, Instant at, String by) {
        this.amount = amount;
        this.updatedAt = at;
        this.updatedBy = by;
    }
}
