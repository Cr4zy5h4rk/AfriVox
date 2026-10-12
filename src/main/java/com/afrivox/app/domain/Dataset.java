package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.DatasetPlan;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Dataset.
 */
@Entity
@Table(name = "dataset")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Dataset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 80)
    @Column(name = "slug", length = 80, nullable = false, unique = true)
    private String slug;

    @NotNull
    @Size(max = 120)
    @Column(name = "title", length = 120, nullable = false)
    private String title;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Size(max = 80)
    @Column(name = "domain", length = 80)
    private String domain;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private DatasetPlan plan;

    /** Quelques clips de l'apercu sont ecoutables par les comptes connectes (reglage de l'admin, faux par defaut). */
    @Column(name = "preview_audio", nullable = false)
    private boolean previewAudio = false;

    @ManyToOne(fetch = FetchType.LAZY)
    private Language language;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Dataset id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isPreviewAudio() {
        return this.previewAudio;
    }

    public void setPreviewAudio(boolean previewAudio) {
        this.previewAudio = previewAudio;
    }

    public String getSlug() {
        return this.slug;
    }

    public Dataset slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return this.title;
    }

    public Dataset title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return this.description;
    }

    public Dataset description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomain() {
        return this.domain;
    }

    public Dataset domain(String domain) {
        this.setDomain(domain);
        return this;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public DatasetPlan getPlan() {
        return this.plan;
    }

    public Dataset plan(DatasetPlan plan) {
        this.setPlan(plan);
        return this;
    }

    public void setPlan(DatasetPlan plan) {
        this.plan = plan;
    }

    public Language getLanguage() {
        return this.language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Dataset language(Language language) {
        this.setLanguage(language);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dataset)) {
            return false;
        }
        return getId() != null && getId().equals(((Dataset) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Dataset{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", title='" + getTitle() + "'" +
            ", description='" + getDescription() + "'" +
            ", domain='" + getDomain() + "'" +
            ", plan='" + getPlan() + "'" +
            "}";
    }
}
