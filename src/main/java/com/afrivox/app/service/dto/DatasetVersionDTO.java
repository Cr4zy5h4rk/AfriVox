package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.afrivox.app.domain.DatasetVersion} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DatasetVersionDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 20)
    private String label;

    @DecimalMin(value = "0")
    private Float hours;

    @Min(value = 0)
    private Integer speakers;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private Float qualityScore;

    @Min(value = 0)
    private Integer pricePerHourFcfa;

    @NotNull
    private LicenseKind license;

    @NotNull
    private VersionStatus status;

    @Size(max = 300)
    private String packagePath;

    @Size(max = 64)
    private String sha256;

    @NotNull
    private Instant createdAt;

    private DatasetDTO dataset;

    private Set<RecordingDTO> recordings = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Float getHours() {
        return hours;
    }

    public void setHours(Float hours) {
        this.hours = hours;
    }

    public Integer getSpeakers() {
        return speakers;
    }

    public void setSpeakers(Integer speakers) {
        this.speakers = speakers;
    }

    public Float getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Float qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getPricePerHourFcfa() {
        return pricePerHourFcfa;
    }

    public void setPricePerHourFcfa(Integer pricePerHourFcfa) {
        this.pricePerHourFcfa = pricePerHourFcfa;
    }

    public LicenseKind getLicense() {
        return license;
    }

    public void setLicense(LicenseKind license) {
        this.license = license;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public DatasetDTO getDataset() {
        return dataset;
    }

    public void setDataset(DatasetDTO dataset) {
        this.dataset = dataset;
    }

    public Set<RecordingDTO> getRecordings() {
        return recordings;
    }

    public void setRecordings(Set<RecordingDTO> recordings) {
        this.recordings = recordings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatasetVersionDTO)) {
            return false;
        }

        DatasetVersionDTO datasetVersionDTO = (DatasetVersionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, datasetVersionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DatasetVersionDTO{" +
            "id=" + getId() +
            ", label='" + getLabel() + "'" +
            ", hours=" + getHours() +
            ", speakers=" + getSpeakers() +
            ", qualityScore=" + getQualityScore() +
            ", pricePerHourFcfa=" + getPricePerHourFcfa() +
            ", license='" + getLicense() + "'" +
            ", status='" + getStatus() + "'" +
            ", packagePath='" + getPackagePath() + "'" +
            ", sha256='" + getSha256() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", dataset=" + getDataset() +
            ", recordings=" + getRecordings() +
            "}";
    }
}
