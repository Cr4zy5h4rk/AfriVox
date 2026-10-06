package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.RequestStatus;
import com.afrivox.app.domain.enumeration.Sector;
import com.afrivox.app.domain.enumeration.UsageKind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A DatasetRequest.
 */
@Entity
@Table(name = "dataset_request")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DatasetRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 120)
    @Column(name = "org_name", length = 120, nullable = false)
    private String orgName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sector", nullable = false)
    private Sector sector;

    @NotNull
    @Min(value = 1)
    @Column(name = "hours", nullable = false)
    private Integer hours;

    @Min(value = 1)
    @Column(name = "speakers_target")
    private Integer speakersTarget;

    @Size(max = 120)
    @Column(name = "conversation_type", length = 120)
    private String conversationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_kind", nullable = false)
    private UsageKind usageKind;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Size(max = 1500)
    @Column(name = "description", length = 1500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    @Min(value = 0)
    @Column(name = "quote_amount_fcfa")
    private Integer quoteAmountFcfa;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile requester;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DatasetRequest id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrgName() {
        return this.orgName;
    }

    public DatasetRequest orgName(String orgName) {
        this.setOrgName(orgName);
        return this;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Sector getSector() {
        return this.sector;
    }

    public DatasetRequest sector(Sector sector) {
        this.setSector(sector);
        return this;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Integer getHours() {
        return this.hours;
    }

    public DatasetRequest hours(Integer hours) {
        this.setHours(hours);
        return this;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Integer getSpeakersTarget() {
        return this.speakersTarget;
    }

    public DatasetRequest speakersTarget(Integer speakersTarget) {
        this.setSpeakersTarget(speakersTarget);
        return this;
    }

    public void setSpeakersTarget(Integer speakersTarget) {
        this.speakersTarget = speakersTarget;
    }

    public String getConversationType() {
        return this.conversationType;
    }

    public DatasetRequest conversationType(String conversationType) {
        this.setConversationType(conversationType);
        return this;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public UsageKind getUsageKind() {
        return this.usageKind;
    }

    public DatasetRequest usageKind(UsageKind usageKind) {
        this.setUsageKind(usageKind);
        return this;
    }

    public void setUsageKind(UsageKind usageKind) {
        this.usageKind = usageKind;
    }

    public LocalDate getDeadline() {
        return this.deadline;
    }

    public DatasetRequest deadline(LocalDate deadline) {
        this.setDeadline(deadline);
        return this;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public String getDescription() {
        return this.description;
    }

    public DatasetRequest description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return this.status;
    }

    public DatasetRequest status(RequestStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Integer getQuoteAmountFcfa() {
        return this.quoteAmountFcfa;
    }

    public DatasetRequest quoteAmountFcfa(Integer quoteAmountFcfa) {
        this.setQuoteAmountFcfa(quoteAmountFcfa);
        return this;
    }

    public void setQuoteAmountFcfa(Integer quoteAmountFcfa) {
        this.quoteAmountFcfa = quoteAmountFcfa;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public DatasetRequest createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public DatasetRequest language(Language language) {
        this.setLanguage(language);
        return this;
    }

    public UserProfile getRequester() {
        return this.requester;
    }

    public void setRequester(UserProfile userProfile) {
        this.requester = userProfile;
    }

    public DatasetRequest requester(UserProfile userProfile) {
        this.setRequester(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatasetRequest)) {
            return false;
        }
        return getId() != null && getId().equals(((DatasetRequest) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DatasetRequest{" +
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
            "}";
    }
}
