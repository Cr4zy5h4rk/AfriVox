package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.CreditReason;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.CreditTransaction} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CreditTransactionDTO implements Serializable {

    private Long id;

    @NotNull
    private Integer amount;

    @NotNull
    private CreditReason reason;

    @NotNull
    private Instant createdAt;

    private UserProfileDTO profile;

    private DatasetVersionDTO datasetVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public CreditReason getReason() {
        return reason;
    }

    public void setReason(CreditReason reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfileDTO getProfile() {
        return profile;
    }

    public void setProfile(UserProfileDTO profile) {
        this.profile = profile;
    }

    public DatasetVersionDTO getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersionDTO datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreditTransactionDTO)) {
            return false;
        }

        CreditTransactionDTO creditTransactionDTO = (CreditTransactionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, creditTransactionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CreditTransactionDTO{" +
            "id=" + getId() +
            ", amount=" + getAmount() +
            ", reason='" + getReason() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", profile=" + getProfile() +
            ", datasetVersion=" + getDatasetVersion() +
            "}";
    }
}
