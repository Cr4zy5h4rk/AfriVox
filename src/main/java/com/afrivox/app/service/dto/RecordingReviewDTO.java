package com.afrivox.app.service.dto;

import com.afrivox.app.domain.enumeration.Verdict;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.afrivox.app.domain.RecordingReview} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingReviewDTO implements Serializable {

    private Long id;

    @NotNull
    private Verdict verdict;

    private Boolean audioOk;

    private Boolean pronunciationOk;

    private Boolean transcriptOk;

    private Boolean gold;

    @Size(max = 300)
    private String note;

    @NotNull
    private Instant createdAt;

    private RecordingDTO recording;

    private UserProfileDTO reviewer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Boolean getAudioOk() {
        return audioOk;
    }

    public void setAudioOk(Boolean audioOk) {
        this.audioOk = audioOk;
    }

    public Boolean getPronunciationOk() {
        return pronunciationOk;
    }

    public void setPronunciationOk(Boolean pronunciationOk) {
        this.pronunciationOk = pronunciationOk;
    }

    public Boolean getTranscriptOk() {
        return transcriptOk;
    }

    public void setTranscriptOk(Boolean transcriptOk) {
        this.transcriptOk = transcriptOk;
    }

    public Boolean getGold() {
        return gold;
    }

    public void setGold(Boolean gold) {
        this.gold = gold;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public RecordingDTO getRecording() {
        return recording;
    }

    public void setRecording(RecordingDTO recording) {
        this.recording = recording;
    }

    public UserProfileDTO getReviewer() {
        return reviewer;
    }

    public void setReviewer(UserProfileDTO reviewer) {
        this.reviewer = reviewer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordingReviewDTO)) {
            return false;
        }

        RecordingReviewDTO recordingReviewDTO = (RecordingReviewDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, recordingReviewDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingReviewDTO{" +
            "id=" + getId() +
            ", verdict='" + getVerdict() + "'" +
            ", audioOk='" + getAudioOk() + "'" +
            ", pronunciationOk='" + getPronunciationOk() + "'" +
            ", transcriptOk='" + getTranscriptOk() + "'" +
            ", gold='" + getGold() + "'" +
            ", note='" + getNote() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", recording=" + getRecording() +
            ", reviewer=" + getReviewer() +
            "}";
    }
}
