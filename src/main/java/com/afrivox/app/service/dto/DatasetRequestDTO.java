package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.DatasetRequest} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DatasetRequestDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 120)
    private String orgName;

    @NotNull
    private Sector sector;

    @NotNull
    @Min(value = 1)
    private Integer hours;

    @Min(value = 1)
    private Integer speakersTarget;

    @Size(max = 120)
    private String conversationType;

    @NotNull
    private UsageKind usageKind;

    private LocalDate deadline;

    @Size(max = 1500)
    private String description;

    @NotNull
    private RequestStatus status;

    @Min(value = 0)
    private Integer quoteAmountFcfa;

    @NotNull
    private Instant createdAt;

    private LanguageDTO language;

    private UserProfileDTO requester;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Integer getSpeakersTarget() {
        return speakersTarget;
    }

    public void setSpeakersTarget(Integer speakersTarget) {
        this.speakersTarget = speakersTarget;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public UsageKind getUsageKind() {
        return usageKind;
    }

    public void setUsageKind(UsageKind usageKind) {
        this.usageKind = usageKind;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Integer getQuoteAmountFcfa() {
        return quoteAmountFcfa;
    }

    public void setQuoteAmountFcfa(Integer quoteAmountFcfa) {
        this.quoteAmountFcfa = quoteAmountFcfa;
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

    public UserProfileDTO getRequester() {
        return requester;
    }

    public void setRequester(UserProfileDTO requester) {
        this.requester = requester;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatasetRequestDTO)) {
            return false;
        }

        DatasetRequestDTO datasetRequestDTO = (DatasetRequestDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, datasetRequestDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DatasetRequestDTO{" +
            "id=" + getId() +
            ", orgName='" + getOrgName() + "'" +
            ", sector='" + getSector() + "'" +
            ", hours=" + getHours() +
            ", speakersTarget=" + getSpeakersTarget() +
            ", conversationType='" + getConversationType() + "'" +
            ", usageKind='" + getUsageKind() + "'" +
            ", deadline='" + getDeadline() + "'" +
            ", description='" + getDescription() + "'" +
            ", status='" + getStatus() + "'" +
            ", quoteAmountFcfa=" + getQuoteAmountFcfa() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", language=" + getLanguage() +
            ", requester=" + getRequester() +
            "}";
    }
}
