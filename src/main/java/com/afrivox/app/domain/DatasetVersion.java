package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.LicenseKind;
import com.afrivox.app.domain.enumeration.VersionStatus;
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
 * A DatasetVersion.
 */
@Entity
@Table(name = "dataset_version")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DatasetVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 20)
    @Column(name = "label", length = 20, nullable = false)
    private String label;

    @DecimalMin(value = "0")
    @Column(name = "hours")
    private Float hours;

    @Min(value = 0)
    @Column(name = "speakers")
    private Integer speakers;

    @DecimalMin(value = "0")
    @DecimalMax(value = "100")
    @Column(name = "quality_score")
    private Float qualityScore;

    @Min(value = 0)
    @Column(name = "price_per_hour_fcfa")
    private Integer pricePerHourFcfa;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "license", nullable = false)
    private LicenseKind license;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VersionStatus status;

    @Size(max = 300)
    @Column(name = "package_path", length = 300)
    private String packagePath;

    @Size(max = 64)
    @Column(name = "sha_256", length = 64)
    private String sha256;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "language" }, allowSetters = true)
    private Dataset dataset;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rel_dataset_version__recording",
        joinColumns = @JoinColumn(name = "dataset_version_id"),
        inverseJoinColumns = @JoinColumn(name = "recording_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "prompt", "contributor", "datasetVersions" }, allowSetters = true)
    private Set<Recording> recordings = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DatasetVersion id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public DatasetVersion label(String label) {
        this.setLabel(label);
        return this;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Float getHours() {
        return this.hours;
    }

    public DatasetVersion hours(Float hours) {
        this.setHours(hours);
        return this;
    }

    public void setHours(Float hours) {
        this.hours = hours;
    }

    public Integer getSpeakers() {
        return this.speakers;
    }

    public DatasetVersion speakers(Integer speakers) {
        this.setSpeakers(speakers);
        return this;
    }

    public void setSpeakers(Integer speakers) {
        this.speakers = speakers;
    }

    public Float getQualityScore() {
        return this.qualityScore;
    }

    public DatasetVersion qualityScore(Float qualityScore) {
        this.setQualityScore(qualityScore);
        return this;
    }

    public void setQualityScore(Float qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getPricePerHourFcfa() {
        return this.pricePerHourFcfa;
    }

    public DatasetVersion pricePerHourFcfa(Integer pricePerHourFcfa) {
        this.setPricePerHourFcfa(pricePerHourFcfa);
        return this;
    }

    public void setPricePerHourFcfa(Integer pricePerHourFcfa) {
        this.pricePerHourFcfa = pricePerHourFcfa;
    }

    public LicenseKind getLicense() {
        return this.license;
    }

    public DatasetVersion license(LicenseKind license) {
        this.setLicense(license);
        return this;
    }

    public void setLicense(LicenseKind license) {
        this.license = license;
    }

    public VersionStatus getStatus() {
        return this.status;
    }

    public DatasetVersion status(VersionStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public String getPackagePath() {
        return this.packagePath;
    }

    public DatasetVersion packagePath(String packagePath) {
        this.setPackagePath(packagePath);
        return this;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public String getSha256() {
        return this.sha256;
    }

    public DatasetVersion sha256(String sha256) {
        this.setSha256(sha256);
        return this;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public DatasetVersion createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion dataset(Dataset dataset) {
        this.setDataset(dataset);
        return this;
    }

    public Set<Recording> getRecordings() {
        return this.recordings;
    }

    public void setRecordings(Set<Recording> recordings) {
        this.recordings = recordings;
    }

    public DatasetVersion recordings(Set<Recording> recordings) {
        this.setRecordings(recordings);
        return this;
    }

    public DatasetVersion addRecording(Recording recording) {
        this.recordings.add(recording);
        return this;
    }

    public DatasetVersion removeRecording(Recording recording) {
        this.recordings.remove(recording);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatasetVersion)) {
            return false;
        }
        return getId() != null && getId().equals(((DatasetVersion) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DatasetVersion{" +
            "id=" + getId() +
            ", label='" + getLabel() + "'" +
            ", hours=" + getHours() +
            ", speakers=" + getSpeakers() +
            ", qualityScore=" + getQualityScore() +
            ", pricePerHourFcfa=" + getPricePerHourFcfa() +
            ", license='" + getLicense() + "'" +
            ", status='" + getStatus() + "'" +
            ", packagePath='" + getPackagePath() + "'" +
            ", sha256='" + getSha256() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
