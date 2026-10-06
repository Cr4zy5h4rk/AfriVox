package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.ConsentKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Consent.
 */
@Entity
@Table(name = "consent")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Consent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private ConsentKind kind;

    @NotNull
    @Size(max = 20)
    @Column(name = "policy_version", length = 20, nullable = false)
    private String policyVersion;

    @NotNull
    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile profile;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Consent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConsentKind getKind() {
        return this.kind;
    }

    public Consent kind(ConsentKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(ConsentKind kind) {
        this.kind = kind;
    }

    public String getPolicyVersion() {
        return this.policyVersion;
    }

    public Consent policyVersion(String policyVersion) {
        this.setPolicyVersion(policyVersion);
        return this;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public Instant getGrantedAt() {
        return this.grantedAt;
    }

    public Consent grantedAt(Instant grantedAt) {
        this.setGrantedAt(grantedAt);
        return this;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }

    public Consent revokedAt(Instant revokedAt) {
        this.setRevokedAt(revokedAt);
        return this;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UserProfile getProfile() {
        return this.profile;
    }

    public void setProfile(UserProfile userProfile) {
        this.profile = userProfile;
    }

    public Consent profile(UserProfile userProfile) {
        this.setProfile(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Consent)) {
            return false;
        }
        return getId() != null && getId().equals(((Consent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Consent{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", policyVersion='" + getPolicyVersion() + "'" +
            ", grantedAt='" + getGrantedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            "}";
    }
}
