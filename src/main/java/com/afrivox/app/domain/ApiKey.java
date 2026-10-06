package com.afrivox.app.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A ApiKey.
 */
@Entity
@Table(name = "api_key")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ApiKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 60)
    @Column(name = "name", length = 60, nullable = false)
    private String name;

    @NotNull
    @Size(max = 12)
    @Column(name = "prefix", length = 12, nullable = false)
    private String prefix;

    @NotNull
    @Size(max = 100)
    @Column(name = "key_hash", length = 100, nullable = false)
    private String keyHash;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile owner;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public ApiKey id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public ApiKey name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public ApiKey prefix(String prefix) {
        this.setPrefix(prefix);
        return this;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getKeyHash() {
        return this.keyHash;
    }

    public ApiKey keyHash(String keyHash) {
        this.setKeyHash(keyHash);
        return this;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public ApiKey createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return this.revokedAt;
    }

    public ApiKey revokedAt(Instant revokedAt) {
        this.setRevokedAt(revokedAt);
        return this;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UserProfile getOwner() {
        return this.owner;
    }

    public void setOwner(UserProfile userProfile) {
        this.owner = userProfile;
    }

    public ApiKey owner(UserProfile userProfile) {
        this.setOwner(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiKey)) {
            return false;
        }
        return getId() != null && getId().equals(((ApiKey) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ApiKey{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", prefix='" + getPrefix() + "'" +
            ", keyHash='" + getKeyHash() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            "}";
    }
}
