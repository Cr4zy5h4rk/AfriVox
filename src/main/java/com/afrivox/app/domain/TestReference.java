package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Transcription de reference d'un enregistrement du jeu de test d'une version. Elle n'est jamais ecrite dans l'archive :
 * un classement dont la reponse est telechargeable ne prouverait rien. Voir {@code DatasetPackagingService}.
 */
@Entity
@Table(name = "test_reference")
public class TestReference {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @NotNull
    @Column(name = "recording_id", nullable = false)
    private Long recordingId;

    @NotNull
    @Size(max = 1000)
    @Column(name = "reference_text", length = 1000, nullable = false)
    private String text;

    protected TestReference() {}

    public TestReference(Long versionId, Long recordingId, String text) {
        this.versionId = versionId;
        this.recordingId = recordingId;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public Long getVersionId() {
        return versionId;
    }

    public Long getRecordingId() {
        return recordingId;
    }

    public String getText() {
        return text;
    }
}
