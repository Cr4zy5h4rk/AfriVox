package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.DatasetPlan;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Dataset} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DatasetDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 80)
    private String slug;

    @NotNull
    @Size(max = 120)
    private String title;

    @Size(max = 1000)
    private String description;

    @Size(max = 80)
    private String domain;

    @NotNull
    private DatasetPlan plan;

    private LanguageDTO language;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public DatasetPlan getPlan() {
        return plan;
    }

    public void setPlan(DatasetPlan plan) {
        this.plan = plan;
    }

    public LanguageDTO getLanguage() {
        return language;
    }

    public void setLanguage(LanguageDTO language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatasetDTO)) {
            return false;
        }

        DatasetDTO datasetDTO = (DatasetDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, datasetDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DatasetDTO{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", domain='" + getDomain() + "'" +
            ", plan='" + getPlan() + "'" +
            ", language=" + getLanguage() +
            "}";
    }
}
