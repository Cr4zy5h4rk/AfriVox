package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Recording.
 */
@Entity
@Table(name = "recording")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Recording implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 300)
    @Column(name = "audio_path", length = 300, nullable = false)
    private String audioPath;

    @NotNull
    @Min(value = 0)
    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Size(max = 64)
    @Column(name = "sha_256", length = 64)
    private String sha256;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "qc_status", nullable = false)
    private QcStatus qcStatus;

    @Column(name = "qc_score")
    private Float qcScore;

    @Size(max = 1000)
    @Column(name = "transcript", length = 1000)
    private String transcript;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecordingStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Credits promis a l'envoi (tarif de la campagne a ce moment) ; {@code null} pour les anciens enregistrements. */
    @Column(name = "reward_credits")
    private Integer rewardCredits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "campaign" }, allowSetters = true)
    private Prompt prompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile contributor;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "recordings")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private Set<DatasetVersion> datasetVersions = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Recording id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAudioPath() {
        return this.audioPath;
    }

    public Recording audioPath(String audioPath) {
        this.setAudioPath(audioPath);
        return this;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public Integer getDurationMs() {
        return this.durationMs;
    }

    public Recording durationMs(Integer durationMs) {
        this.setDurationMs(durationMs);
        return this;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public String getSha256() {
        return this.sha256;
    }

    public Recording sha256(String sha256) {
        this.setSha256(sha256);
        return this;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public QcStatus getQcStatus() {
        return this.qcStatus;
    }

    public Recording qcStatus(QcStatus qcStatus) {
        this.setQcStatus(qcStatus);
        return this;
    }

    public void setQcStatus(QcStatus qcStatus) {
        this.qcStatus = qcStatus;
    }

    public Float getQcScore() {
        return this.qcScore;
    }

    public Recording qcScore(Float qcScore) {
        this.setQcScore(qcScore);
        return this;
    }

    public void setQcScore(Float qcScore) {
        this.qcScore = qcScore;
    }

    public String getTranscript() {
        return this.transcript;
    }

    public Recording transcript(String transcript) {
        this.setTranscript(transcript);
        return this;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public RecordingStatus getStatus() {
        return this.status;
    }

    public Recording status(RecordingStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(RecordingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Recording createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRewardCredits() {
        return this.rewardCredits;
    }

    public void setRewardCredits(Integer rewardCredits) {
        this.rewardCredits = rewardCredits;
    }

    public Prompt getPrompt() {
        return this.prompt;
    }

    public void setPrompt(Prompt prompt) {
        this.prompt = prompt;
    }

    public Recording prompt(Prompt prompt) {
        this.setPrompt(prompt);
        return this;
    }

    public UserProfile getContributor() {
        return this.contributor;
    }

    public void setContributor(UserProfile userProfile) {
        this.contributor = userProfile;
    }

    public Recording contributor(UserProfile userProfile) {
        this.setContributor(userProfile);
        return this;
    }

    public Set<DatasetVersion> getDatasetVersions() {
        return this.datasetVersions;
    }

    public void setDatasetVersions(Set<DatasetVersion> datasetVersions) {
        if (this.datasetVersions != null) {
            this.datasetVersions.forEach(i -> i.removeRecording(this));
        }
        if (datasetVersions != null) {
            datasetVersions.forEach(i -> i.addRecording(this));
        }
        this.datasetVersions = datasetVersions;
    }

    public Recording datasetVersions(Set<DatasetVersion> datasetVersions) {
        this.setDatasetVersions(datasetVersions);
        return this;
    }

    public Recording addDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersions.add(datasetVersion);
        datasetVersion.getRecordings().add(this);
        return this;
    }

    public Recording removeDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersions.remove(datasetVersion);
        datasetVersion.getRecordings().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recording)) {
            return false;
        }
        return getId() != null && getId().equals(((Recording) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Recording{" +
            "id=" + getId() +
            ", audioPath='" + getAudioPath() + "'" +
            ", durationMs=" + getDurationMs() +
            ", sha256='" + getSha256() + "'" +
            ", qcStatus='" + getQcStatus() + "'" +
            ", qcScore=" + getQcScore() +
            ", transcript='" + getTranscript() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
