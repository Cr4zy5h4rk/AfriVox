package com.afrivox.app.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.Evaluation} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EvaluationDTO implements Serializable {

    private Long id;

    @DecimalMin(value = "0")
    private Float wer;

    @DecimalMin(value = "0")
    private Float cer;

    @NotNull
    private Instant submittedAt;

    private MlModelDTO model;

    private DatasetVersionDTO testSet;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getWer() {
        return wer;
    }

    public void setWer(Float wer) {
        this.wer = wer;
    }

    public Float getCer() {
        return cer;
    }

    public void setCer(Float cer) {
        this.cer = cer;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public MlModelDTO getModel() {
        return model;
    }

    public void setModel(MlModelDTO model) {
        this.model = model;
    }

    public DatasetVersionDTO getTestSet() {
        return testSet;
    }

    public void setTestSet(DatasetVersionDTO testSet) {
        this.testSet = testSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluationDTO)) {
            return false;
        }

        EvaluationDTO evaluationDTO = (EvaluationDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, evaluationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EvaluationDTO{" +
            "id=" + getId() +
            ", wer=" + getWer() +
            ", cer=" + getCer() +
            ", submittedAt='" + getSubmittedAt() + "'" +
            ", model=" + getModel() +
            ", testSet=" + getTestSet() +
            "}";
    }
}
