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
 * A DownloadLog.
 */
@Entity
@Table(name = "download_log")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DownloadLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt;

    @Size(max = 45)
    @Column(name = "ip", length = 45)
    private String ip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "dataset", "recordings" }, allowSetters = true)
    private DatasetVersion datasetVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "user" }, allowSetters = true)
    private UserProfile profile;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public DownloadLog id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getDownloadedAt() {
        return this.downloadedAt;
    }

    public DownloadLog downloadedAt(Instant downloadedAt) {
        this.setDownloadedAt(downloadedAt);
        return this;
    }

    public void setDownloadedAt(Instant downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public String getIp() {
        return this.ip;
    }

    public DownloadLog ip(String ip) {
        this.setIp(ip);
        return this;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public DatasetVersion getDatasetVersion() {
        return this.datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    public DownloadLog datasetVersion(DatasetVersion datasetVersion) {
        this.setDatasetVersion(datasetVersion);
        return this;
    }

    public UserProfile getProfile() {
        return this.profile;
    }

    public void setProfile(UserProfile userProfile) {
        this.profile = userProfile;
    }

    public DownloadLog profile(UserProfile userProfile) {
        this.setProfile(userProfile);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DownloadLog)) {
            return false;
        }
        return getId() != null && getId().equals(((DownloadLog) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DownloadLog{" +
            "id=" + getId() +
            ", downloadedAt='" + getDownloadedAt() + "'" +
            ", ip='" + getIp() + "'" +
            "}";
    }
}
