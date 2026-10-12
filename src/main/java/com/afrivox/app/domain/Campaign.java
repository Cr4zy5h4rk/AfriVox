package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.CampaignKind;
import com.afrivox.app.domain.enumeration.CampaignStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Campaign.
 */
@Entity
@Table(name = "campaign")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Campaign implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 120)
    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Size(max = 80)
    @Column(name = "domain", length = 80)
    private String domain;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private CampaignKind kind;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CampaignStatus status;

    @Min(value = 1)
    @Column(name = "target_sentences")
    private Integer targetSentences;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Tarifs propres a la campagne, en credits ; {@code null} = tarif global (voir {@code CreditService}). */
    @Min(value = 0)
    @Max(value = 1000)
    @Column(name = "credit_validated_recording")
    private Integer creditValidatedRecording;

    @Min(value = 0)
    @Max(value = 1000)
    @Column(name = "credit_review_agreed")
    private Integer creditReviewAgreed;

    @Min(value = 0)
    @Max(value = 1000)
    @Column(name = "credit_trap_correct")
    private Integer creditTrapCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "language", "requester" }, allowSetters = true)
    private DatasetRequest request;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Campaign id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Campaign title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Campaign description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomain() {
        return this.domain;
    }

    public Campaign domain(String domain) {
        this.setDomain(domain);
        return this;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public CampaignKind getKind() {
        return this.kind;
    }

    public Campaign kind(CampaignKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(CampaignKind kind) {
        this.kind = kind;
    }

    public CampaignStatus getStatus() {
        return this.status;
    }

    public Campaign status(CampaignStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public Integer getTargetSentences() {
        return this.targetSentences;
    }

    public Campaign targetSentences(Integer targetSentences) {
        this.setTargetSentences(targetSentences);
        return this;
    }

    public void setTargetSentences(Integer targetSentences) {
        this.targetSentences = targetSentences;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Campaign createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCreditValidatedRecording() {
        return this.creditValidatedRecording;
    }

    public void setCreditValidatedRecording(Integer creditValidatedRecording) {
        this.creditValidatedRecording = creditValidatedRecording;
    }

    public Integer getCreditReviewAgreed() {
        return this.creditReviewAgreed;
    }

    public void setCreditReviewAgreed(Integer creditReviewAgreed) {
        this.creditReviewAgreed = creditReviewAgreed;
    }

    public Integer getCreditTrapCorrect() {
        return this.creditTrapCorrect;
    }

    public void setCreditTrapCorrect(Integer creditTrapCorrect) {
        this.creditTrapCorrect = creditTrapCorrect;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Campaign language(Language language) {
        this.setLanguage(language);
        return this;
    }

    public DatasetRequest getRequest() {
        return this.request;
    }

    public void setRequest(DatasetRequest datasetRequest) {
        this.request = datasetRequest;
    }

    public Campaign request(DatasetRequest datasetRequest) {
        this.setRequest(datasetRequest);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Campaign)) {
            return false;
        }
        return getId() != null && getId().equals(((Campaign) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Campaign{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", domain='" + getDomain() + "'" +
            ", kind='" + getKind() + "'" +
            ", status='" + getStatus() + "'" +
            ", targetSentences=" + getTargetSentences() +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
