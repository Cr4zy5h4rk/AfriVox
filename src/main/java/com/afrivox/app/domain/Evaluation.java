package com.afrivox.app.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Evaluation.
 */
@Entity
@Table(name = "evaluation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Evaluation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @DecimalMin(value = "0")
    @Column(name = "wer")
    private Float wer;

    @DecimalMin(value = "0")
    @Column(name = "cer")
    private Float cer;

    @NotNull
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "owner", "language", "trainedOn" }, allowSetters = true)
    private MlModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private DatasetVersion testSet;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Evaluation id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getWer() {
        return this.wer;
    }

    public Evaluation wer(Float wer) {
        this.setWer(wer);
        return this;
    }

    public void setWer(Float wer) {
        this.wer = wer;
    }

    public Float getCer() {
        return this.cer;
    }

    public Evaluation cer(Float cer) {
        this.setCer(cer);
        return this;
    }

    public void setCer(Float cer) {
        this.cer = cer;
    }

    public Instant getSubmittedAt() {
        return this.submittedAt;
    }

    public Evaluation submittedAt(Instant submittedAt) {
        this.setSubmittedAt(submittedAt);
        return this;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public MlModel getModel() {
        return this.model;
    }

    public void setModel(MlModel mlModel) {
        this.model = mlModel;
    }

    public Evaluation model(MlModel mlModel) {
        this.setModel(mlModel);
        return this;
    }

    public DatasetVersion getTestSet() {
        return this.testSet;
    }

    public void setTestSet(DatasetVersion datasetVersion) {
        this.testSet = datasetVersion;
    }

    public Evaluation testSet(DatasetVersion datasetVersion) {
        this.setTestSet(datasetVersion);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Evaluation)) {
            return false;
        }
        return getId() != null && getId().equals(((Evaluation) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Evaluation{" +
            "id=" + getId() +
            ", wer=" + getWer() +
            ", cer=" + getCer() +
            ", submittedAt='" + getSubmittedAt() + "'" +
            "}";
    }
}
