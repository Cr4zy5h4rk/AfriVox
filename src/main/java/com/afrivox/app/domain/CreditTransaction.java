package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.CreditReason;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A CreditTransaction.
 */
@Entity
@Table(name = "credit_transaction")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CreditTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private CreditReason reason;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private DatasetVersion datasetVersion;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public CreditTransaction id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAmount() {
        return this.amount;
    }

    public CreditTransaction amount(Integer amount) {
        this.setAmount(amount);
        return this;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public CreditReason getReason() {
        return this.reason;
    }

    public CreditTransaction reason(CreditReason reason) {
        this.setReason(reason);
        return this;
    }

    public void setReason(CreditReason reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public CreditTransaction createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfile getProfile() {
        return this.profile;
    }

    public void setProfile(UserProfile userProfile) {
        this.profile = userProfile;
    }

    public CreditTransaction profile(UserProfile userProfile) {
        this.setProfile(userProfile);
        return this;
    }

    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public CreditTransaction datasetVersion(DatasetVersion datasetVersion) {
        this.setDatasetVersion(datasetVersion);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreditTransaction)) {
            return false;
        }
        return getId() != null && getId().equals(((CreditTransaction) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CreditTransaction{" +
            "id=" + getId() +
            ", amount=" + getAmount() +
            ", reason='" + getReason() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
