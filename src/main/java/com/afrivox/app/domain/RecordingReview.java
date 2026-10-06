package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.Verdict;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A RecordingReview.
 */
@Entity
@Table(name = "recording_review")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RecordingReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false)
    private Verdict verdict;

    @Column(name = "audio_ok")
    private Boolean audioOk;

    @Column(name = "pronunciation_ok")
    private Boolean pronunciationOk;

    @Column(name = "transcript_ok")
    private Boolean transcriptOk;

    @Column(name = "gold")
    private Boolean gold;

    @Size(max = 300)
    @Column(name = "note", length = 300)
    private String note;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "prompt", "contributor", "datasetVersions" }, allowSetters = true)
    private Recording recording;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile reviewer;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public RecordingReview id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Verdict getVerdict() {
        return this.verdict;
    }

    public RecordingReview verdict(Verdict verdict) {
        this.setVerdict(verdict);
        return this;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public Boolean getAudioOk() {
        return this.audioOk;
    }

    public RecordingReview audioOk(Boolean audioOk) {
        this.setAudioOk(audioOk);
        return this;
    }

    public void setAudioOk(Boolean audioOk) {
        this.audioOk = audioOk;
    }

    public Boolean getPronunciationOk() {
        return this.pronunciationOk;
    }

    public RecordingReview pronunciationOk(Boolean pronunciationOk) {
        this.setPronunciationOk(pronunciationOk);
        return this;
    }

    public void setPronunciationOk(Boolean pronunciationOk) {
        this.pronunciationOk = pronunciationOk;
    }

    public Boolean getTranscriptOk() {
        return this.transcriptOk;
    }

    public RecordingReview transcriptOk(Boolean transcriptOk) {
        this.setTranscriptOk(transcriptOk);
        return this;
    }

    public void setTranscriptOk(Boolean transcriptOk) {
        this.transcriptOk = transcriptOk;
    }

    public Boolean getGold() {
        return this.gold;
    }

    public RecordingReview gold(Boolean gold) {
        this.setGold(gold);
        return this;
    }

    public void setGold(Boolean gold) {
        this.gold = gold;
    }

    public String getNote() {
        return this.note;
    }

    public RecordingReview note(String note) {
        this.setNote(note);
        return this;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public RecordingReview createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Recording getRecording() {
        return this.recording;
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public RecordingReview recording(Recording recording) {
        this.setRecording(recording);
        return this;
    }

    public UserProfile getReviewer() {
        return this.reviewer;
    }

    public void setReviewer(UserProfile userProfile) {
        this.reviewer = userProfile;
    }

    public RecordingReview reviewer(UserProfile userProfile) {
        this.setReviewer(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RecordingReview)) {
            return false;
        }
        return getId() != null && getId().equals(((RecordingReview) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "RecordingReview{" +
            "id=" + getId() +
            ", verdict='" + getVerdict() + "'" +
            ", audioOk='" + getAudioOk() + "'" +
            ", pronunciationOk='" + getPronunciationOk() + "'" +
            ", transcriptOk='" + getTranscriptOk() + "'" +
            ", gold='" + getGold() + "'" +
            ", note='" + getNote() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
