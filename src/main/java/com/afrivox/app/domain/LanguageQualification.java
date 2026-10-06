package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.QualificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A LanguageQualification.
 */
@Entity
@Table(name = "language_qualification")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LanguageQualification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "score")
    private Float score;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QualificationStatus status;

    @Column(name = "qualified_at")
    private Instant qualifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    private Language language;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public LanguageQualification id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getScore() {
        return this.score;
    }

    public LanguageQualification score(Float score) {
        this.setScore(score);
        return this;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public QualificationStatus getStatus() {
        return this.status;
    }

    public LanguageQualification status(QualificationStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(QualificationStatus status) {
        this.status = status;
    }

    public Instant getQualifiedAt() {
        return this.qualifiedAt;
    }

    public LanguageQualification qualifiedAt(Instant qualifiedAt) {
        this.setQualifiedAt(qualifiedAt);
        return this;
    }

    public void setQualifiedAt(Instant qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public UserProfile getProfile() {
        return this.profile;
    }

    public void setProfile(UserProfile userProfile) {
        this.profile = userProfile;
    }

    public LanguageQualification profile(UserProfile userProfile) {
        this.setProfile(userProfile);
        return this;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public LanguageQualification language(Language language) {
        this.setLanguage(language);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LanguageQualification)) {
            return false;
        }
        return getId() != null && getId().equals(((LanguageQualification) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LanguageQualification{" +
            "id=" + getId() +
            ", score=" + getScore() +
            ", status='" + getStatus() + "'" +
            ", qualifiedAt='" + getQualifiedAt() + "'" +
            "}";
    }
}
