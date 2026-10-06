package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.MlTask;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.MlModel} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MlModelDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 120)
    private String name;

    @NotNull
    private MlTask task;

    @Size(max = 1000)
    private String description;

    @Size(max = 60)
    private String license;

    @Size(max = 300)
    private String url;

    @Min(value = 0)
    private Integer downloads;

    @NotNull
    private Instant createdAt;

    private UserProfileDTO owner;

    private LanguageDTO language;

    private DatasetVersionDTO trainedOn;

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

    public MlTask getTask() {
        return task;
    }

    public void setTask(MlTask task) {
        this.task = task;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getDownloads() {
        return downloads;
    }

    public void setDownloads(Integer downloads) {
        this.downloads = downloads;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfileDTO getOwner() {
        return owner;
    }

    public void setOwner(UserProfileDTO owner) {
        this.owner = owner;
    }

    public LanguageDTO getLanguage() {
        return language;
    }

    public void setLanguage(LanguageDTO language) {
        this.language = language;
    }

    public DatasetVersionDTO getTrainedOn() {
        return trainedOn;
    }

    public void setTrainedOn(DatasetVersionDTO trainedOn) {
        this.trainedOn = trainedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MlModelDTO)) {
            return false;
        }

        MlModelDTO mlModelDTO = (MlModelDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, mlModelDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MlModelDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", task='" + getTask() + "'" +
            ", description='" + getDescription() + "'" +
            ", license='" + getLicense() + "'" +
            ", url='" + getUrl() + "'" +
            ", downloads=" + getDownloads() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", owner=" + getOwner() +
            ", language=" + getLanguage() +
            ", trainedOn=" + getTrainedOn() +
            "}";
    }
}
