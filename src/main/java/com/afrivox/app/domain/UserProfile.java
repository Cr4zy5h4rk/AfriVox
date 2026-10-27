package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A UserProfile.
 */
@Entity
@Table(name = "user_profile")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 80)
    @Column(name = "display_name", length = 80, nullable = false)
    private String displayName;

    @Size(max = 60)
    @Column(name = "country", length = 60)
    private String country;

    @Size(max = 300)
    @Column(name = "bio", length = 300)
    private String bio;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Periode d'essai : voir {@link com.afrivox.app.service.ContributorTrialService}. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "contributor_status", length = 20, nullable = false)
    private com.afrivox.app.domain.enumeration.ContributorStatus contributorStatus =
        com.afrivox.app.domain.enumeration.ContributorStatus.ACTIVE;

    /** Date de la confirmation « j'ai 18 ans ou plus » faite a l'inscription (vide pour les profils plus anciens). */
    @Column(name = "adult_confirmed_at")
    private Instant adultConfirmedAt;

    /** Code d'une langue du catalogue, ou {@code other}. Declaratif. */
    @Size(max = 20)
    @Column(name = "native_language", length = 20)
    private String nativeLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_range", length = 10)
    private com.afrivox.app.domain.enumeration.AgeRange ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private com.afrivox.app.domain.enumeration.Gender gender;

    /** Region ou ville d'origine, texte libre : jamais publie, sert seulement a l'equipe. */
    @Size(max = 60)
    @Column(name = "region", length = 60)
    private String region;

    /** Codes de langues separes par des virgules. */
    @Size(max = 120)
    @Column(name = "other_languages", length = 120)
    private String otherLanguages;

    /** Renseigne quand le contributeur a repondu (ou passe) le questionnaire : il n'est alors plus propose. */
    @Column(name = "demographics_answered_at")
    private Instant demographicsAnsweredAt;

    /** Profil visible de tous (annuaire des developpeurs). Desactive par defaut : rien n'est public sans choix explicite. */
    @Column(name = "public_profile", nullable = false)
    private boolean publicProfile = false;

    /** Accepte les demandes de contact des organisations ; sans effet si le profil n'est pas public. */
    @Column(name = "open_to_work", nullable = false)
    private boolean openToWork = false;

    /** Lien https choisi par la personne (site, depot, profil professionnel). */
    @Size(max = 300)
    @Column(name = "profile_url", length = 300)
    private String profileUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(unique = true)
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public UserProfile id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public UserProfile displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCountry() {
        return this.country;
    }

    public UserProfile country(String country) {
        this.setCountry(country);
        return this;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBio() {
        return this.bio;
    }

    public UserProfile bio(String bio) {
        this.setBio(bio);
        return this;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public UserProfile createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getAdultConfirmedAt() {
        return adultConfirmedAt;
    }

    public void setAdultConfirmedAt(Instant adultConfirmedAt) {
        this.adultConfirmedAt = adultConfirmedAt;
    }

    public String getNativeLanguage() {
        return nativeLanguage;
    }

    public void setNativeLanguage(String nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }

    public com.afrivox.app.domain.enumeration.AgeRange getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(com.afrivox.app.domain.enumeration.AgeRange ageRange) {
        this.ageRange = ageRange;
    }

    public com.afrivox.app.domain.enumeration.Gender getGender() {
        return gender;
    }

    public void setGender(com.afrivox.app.domain.enumeration.Gender gender) {
        this.gender = gender;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getOtherLanguages() {
        return otherLanguages;
    }

    public void setOtherLanguages(String otherLanguages) {
        this.otherLanguages = otherLanguages;
    }

    public boolean isPublicProfile() {
        return publicProfile;
    }

    public void setPublicProfile(boolean publicProfile) {
        this.publicProfile = publicProfile;
    }

    public boolean isOpenToWork() {
        return openToWork;
    }

    public void setOpenToWork(boolean openToWork) {
        this.openToWork = openToWork;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public Instant getDemographicsAnsweredAt() {
        return demographicsAnsweredAt;
    }

    public void setDemographicsAnsweredAt(Instant demographicsAnsweredAt) {
        this.demographicsAnsweredAt = demographicsAnsweredAt;
    }

    public com.afrivox.app.domain.enumeration.ContributorStatus getContributorStatus() {
        return this.contributorStatus;
    }

    public void setContributorStatus(com.afrivox.app.domain.enumeration.ContributorStatus contributorStatus) {
        this.contributorStatus = contributorStatus;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserProfile user(User user) {
        this.setUser(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserProfile)) {
            return false;
        }
        return getId() != null && getId().equals(((UserProfile) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserProfile{" +
            "id=" + getId() +
            ", displayName='" + getDisplayName() + "'" +
            ", country='" + getCountry() + "'" +
            ", bio='" + getBio() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
