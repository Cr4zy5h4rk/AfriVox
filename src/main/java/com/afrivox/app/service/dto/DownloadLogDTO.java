package com.afrivox.app.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.DownloadLog} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DownloadLogDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant downloadedAt;

    @Size(max = 45)
    private String ip;

    private DatasetVersionDTO datasetVersion;

    private UserProfileDTO profile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(Instant downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public DatasetVersionDTO getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersionDTO datasetVersion) {
        this.datasetVersion = datasetVersion;
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
        if (!(o instanceof DownloadLogDTO)) {
            return false;
        }

        DownloadLogDTO downloadLogDTO = (DownloadLogDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, downloadLogDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DownloadLogDTO{" +
            "id=" + getId() +
            ", downloadedAt='" + getDownloadedAt() + "'" +
            ", ip='" + getIp() + "'" +
            ", datasetVersion=" + getDatasetVersion() +
            ", profile=" + getProfile() +
            "}";
    }
}
