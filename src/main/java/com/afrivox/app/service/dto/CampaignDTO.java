package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Campaign} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class CampaignDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 120)
    private String title;

    @Size(max = 1000)
    private String description;

    @Size(max = 80)
    private String domain;

    @NotNull
    private CampaignKind kind;

    @NotNull
    private CampaignStatus status;

    @Min(value = 1)
    private Integer targetSentences;

    @NotNull
    private Instant createdAt;

    private LanguageDTO language;

    private DatasetRequestDTO request;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CampaignKind getKind() {
        return kind;
    }

    public void setKind(CampaignKind kind) {
        this.kind = kind;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public Integer getTargetSentences() {
        return targetSentences;
    }

    public void setTargetSentences(Integer targetSentences) {
        this.targetSentences = targetSentences;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LanguageDTO getLanguage() {
        return language;
    }

    public void setLanguage(LanguageDTO language) {
        this.language = language;
    }

    public DatasetRequestDTO getRequest() {
        return request;
    }

    public void setRequest(DatasetRequestDTO request) {
        this.request = request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CampaignDTO)) {
            return false;
        }

        CampaignDTO campaignDTO = (CampaignDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, campaignDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "CampaignDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", domain='" + getDomain() + "'" +
            ", kind='" + getKind() + "'" +
            ", status='" + getStatus() + "'" +
            ", targetSentences=" + getTargetSentences() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", language=" + getLanguage() +
            ", request=" + getRequest() +
            "}";
    }
}
