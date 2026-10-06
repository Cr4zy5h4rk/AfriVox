package com.afrivox.app.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.ApiKey} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ApiKeyDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 60)
    private String name;

    @NotNull
    @Size(max = 12)
    private String prefix;

    @NotNull
    @Size(max = 100)
    private String keyHash;

    @NotNull
    private Instant createdAt;

    private Instant revokedAt;

    private UserProfileDTO owner;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UserProfileDTO getOwner() {
        return owner;
    }

    public void setOwner(UserProfileDTO owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApiKeyDTO)) {
            return false;
        }

        ApiKeyDTO apiKeyDTO = (ApiKeyDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, apiKeyDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ApiKeyDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", prefix='" + getPrefix() + "'" +
            ", keyHash='" + getKeyHash() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", owner=" + getOwner() +
            "}";
    }
}
