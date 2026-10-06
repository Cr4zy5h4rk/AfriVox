package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.ConsentKind;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Consent} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ConsentDTO implements Serializable {

    private Long id;

    @NotNull
    private ConsentKind kind;

    @NotNull
    @Size(max = 20)
    private String policyVersion;

    @NotNull
    private Instant grantedAt;

    private Instant revokedAt;

    private UserProfileDTO profile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ConsentKind getKind() {
        return kind;
    }

    public void setKind(ConsentKind kind) {
        this.kind = kind;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public UserProfileDTO getProfile() {
        return profile;
    }

    public void setProfile(UserProfileDTO profile) {
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsentDTO)) {
            return false;
        }

        ConsentDTO consentDTO = (ConsentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, consentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ConsentDTO{" +
            "id=" + getId() +
            ", kind='" + getKind() + "'" +
            ", policyVersion='" + getPolicyVersion() + "'" +
            ", grantedAt='" + getGrantedAt() + "'" +
            ", revokedAt='" + getRevokedAt() + "'" +
            ", profile=" + getProfile() +
            "}";
    }
}
