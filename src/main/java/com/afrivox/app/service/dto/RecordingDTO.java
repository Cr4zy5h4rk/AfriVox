package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.QcStatus;
import com.afrivox.app.domain.enumeration.RecordingStatus;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.afrivox.app.domain.Recording} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 300)
    private String audioPath;

    @NotNull
    @Min(value = 0)
    private Integer durationMs;

    @Size(max = 64)
    private String sha256;

    @NotNull
    private QcStatus qcStatus;

    private Float qcScore;

    @Size(max = 1000)
    private String transcript;

    @NotNull
    private RecordingStatus status;

    @NotNull
    private Instant createdAt;

    private PromptDTO prompt;

    private UserProfileDTO contributor;

    private Set<DatasetVersionDTO> datasetVersions = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public QcStatus getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(QcStatus qcStatus) {
        this.qcStatus = qcStatus;
    }

    public Float getQcScore() {
        return qcScore;
    }

    public void setQcScore(Float qcScore) {
        this.qcScore = qcScore;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public RecordingStatus getStatus() {
        return status;
    }

    public void setStatus(RecordingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PromptDTO getPrompt() {
        return prompt;
    }

    public void setPrompt(PromptDTO prompt) {
        this.prompt = prompt;
    }

    public UserProfileDTO getContributor() {
        return contributor;
    }

    public void setContributor(UserProfileDTO contributor) {
        this.contributor = contributor;
    }

    public Set<DatasetVersionDTO> getDatasetVersions() {
        return datasetVersions;
    }

    public void setDatasetVersions(Set<DatasetVersionDTO> datasetVersions) {
        this.datasetVersions = datasetVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordingDTO)) {
            return false;
        }

        RecordingDTO recordingDTO = (RecordingDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recordingDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingDTO{" +
            "id=" + getId() +
            ", audioPath='" + getAudioPath() + "'" +
            ", durationMs=" + getDurationMs() +
            ", sha256='" + getSha256() + "'" +
            ", qcStatus='" + getQcStatus() + "'" +
            ", qcScore=" + getQcScore() +
            ", transcript='" + getTranscript() + "'" +
            ", status='" + getStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", prompt=" + getPrompt() +
            ", contributor=" + getContributor() +
            ", datasetVersions=" + getDatasetVersions() +
            "}";
    }
}
