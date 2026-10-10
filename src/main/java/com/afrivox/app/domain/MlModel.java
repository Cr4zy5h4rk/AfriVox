package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.MlTask;
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
 * A MlModel.
 */
@Entity
@Table(name = "ml_model")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MlModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 120)
    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "task", nullable = false)
    private MlTask task;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Size(max = 60)
    @Column(name = "license", length = 60)
    private String license;

    @Size(max = 300)
    @Column(name = "url", length = 300)
    private String url;

    /** Empreinte SHA-256 (hex, 64 caracteres) du fichier du modele, declarative et facultative. */
    @Size(max = 64)
    @Column(name = "sha256", length = 64)
    private String sha256;

    @Min(value = 0)
    @Column(name = "downloads")
    private Integer downloads;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile owner;

    @ManyToOne(fetch = FetchType.LAZY)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private DatasetVersion trainedOn;

    /**
     * Autres versions de datasets utilisees pour l'entrainement, en plus du dataset principal {@link #trainedOn}.
     * Declaratif : le developpeur indique ce qu'il a utilise, la plateforme ne peut pas le verifier.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_ml_model__other_dataset",
        joinColumns = @JoinColumn(name = "ml_model_id"),
        inverseJoinColumns = @JoinColumn(name = "dataset_version_id")
    )
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private Set<DatasetVersion> otherDatasets = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public MlModel id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public MlModel name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MlTask getTask() {
        return this.task;
    }

    public MlModel task(MlTask task) {
        this.setTask(task);
        return this;
    }

    public void setTask(MlTask task) {
        this.task = task;
    }

    public String getDescription() {
        return this.description;
    }

    public MlModel description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicense() {
        return this.license;
    }

    public MlModel license(String license) {
        this.setLicense(license);
        return this;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getUrl() {
        return this.url;
    }

    public MlModel url(String url) {
        this.setUrl(url);
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSha256() {
        return this.sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Integer getDownloads() {
        return this.downloads;
    }

    public MlModel downloads(Integer downloads) {
        this.setDownloads(downloads);
        return this;
    }

    public void setDownloads(Integer downloads) {
        this.downloads = downloads;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public MlModel createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfile getOwner() {
        return this.owner;
    }

    public void setOwner(UserProfile userProfile) {
        this.owner = userProfile;
    }

    public MlModel owner(UserProfile userProfile) {
        this.setOwner(userProfile);
        return this;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public MlModel language(Language language) {
        this.setLanguage(language);
        return this;
    }

    public DatasetVersion getTrainedOn() {
        return this.trainedOn;
    }

    public void setTrainedOn(DatasetVersion datasetVersion) {
        this.trainedOn = datasetVersion;
    }

    public MlModel trainedOn(DatasetVersion datasetVersion) {
        this.setTrainedOn(datasetVersion);
        return this;
    }

    public Set<DatasetVersion> getOtherDatasets() {
        return this.otherDatasets;
    }

    public void setOtherDatasets(Set<DatasetVersion> otherDatasets) {
        this.otherDatasets = otherDatasets;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MlModel)) {
            return false;
        }
        return getId() != null && getId().equals(((MlModel) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MlModel{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", task='" + getTask() + "'" +
            ", description='" + getDescription() + "'" +
            ", license='" + getLicense() + "'" +
            ", url='" + getUrl() + "'" +
            ", downloads=" + getDownloads() +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
