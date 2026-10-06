package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.QualificationStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.LanguageQualification} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class LanguageQualificationDTO implements Serializable {

    private Long id;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    private Float score;

    @NotNull
    private QualificationStatus status;

    private Instant qualifiedAt;

    private UserProfileDTO profile;

    private LanguageDTO language;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public QualificationStatus getStatus() {
        return status;
    }

    public void setStatus(QualificationStatus status) {
        this.status = status;
    }

    public Instant getQualifiedAt() {
        return qualifiedAt;
    }

    public void setQualifiedAt(Instant qualifiedAt) {
        this.qualifiedAt = qualifiedAt;
    }

    public UserProfileDTO getProfile() {
        return profile;
    }

    public void setProfile(UserProfileDTO profile) {
        this.profile = profile;
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
        if (!(o instanceof LanguageQualificationDTO)) {
            return false;
        }

        LanguageQualificationDTO languageQualificationDTO = (LanguageQualificationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, languageQualificationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "LanguageQualificationDTO{" +
            "id=" + getId() +
            ", score=" + getScore() +
            ", status='" + getStatus() + "'" +
            ", qualifiedAt='" + getQualifiedAt() + "'" +
            ", profile=" + getProfile() +
            ", language=" + getLanguage() +
            "}";
    }
}
