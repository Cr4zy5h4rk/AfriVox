package com.afrivox.app.domain;

import com.afrivox.app.domain.enumeration.LicenseKind;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Achat d'une version de dataset sous une licence. L'acceptation de la licence (date, version du texte, compte)
 * est enregistree ici : sans achat il n'y a pas d'acceptation, et sans acceptation pas d'achat.
 */
@Entity
@Table(name = "purchase")
public class Purchase implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "license", nullable = false)
    private LicenseKind license;

    /** Prix paye en FCFA, fige a l'achat (0 pour le plan Starter). */
    @NotNull
    @Min(0)
    @Column(name = "price_fcfa", nullable = false)
    private Long priceFcfa;

    @NotNull
    @Size(max = 40)
    @Column(name = "license_version", length = 40, nullable = false)
    private String licenseVersion;

    @NotNull
    @Column(name = "license_accepted_at", nullable = false)
    private Instant licenseAcceptedAt;

    /** Reference du paiement simule ; vide quand l'achat est gratuit. */
    @Size(max = 60)
    @Column(name = "payment_ref", length = 60)
    private String paymentRef;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private UserProfile profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private DatasetVersion datasetVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LicenseKind getLicense() {
        return license;
    }

    public void setLicense(LicenseKind license) {
        this.license = license;
    }

    public Long getPriceFcfa() {
        return priceFcfa;
    }

    public void setPriceFcfa(Long priceFcfa) {
        this.priceFcfa = priceFcfa;
    }

    public String getLicenseVersion() {
        return licenseVersion;
    }

    public void setLicenseVersion(String licenseVersion) {
        this.licenseVersion = licenseVersion;
    }

    public Instant getLicenseAcceptedAt() {
        return licenseAcceptedAt;
    }

    public void setLicenseAcceptedAt(Instant licenseAcceptedAt) {
        this.licenseAcceptedAt = licenseAcceptedAt;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public void setPaymentRef(String paymentRef) {
        this.paymentRef = paymentRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Purchase)) {
            return false;
        }
        return getId() != null && getId().equals(((Purchase) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Purchase{id=" + id + ", license=" + license + ", priceFcfa=" + priceFcfa + "}";
    }
}
