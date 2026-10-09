package com.afrivox.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Lien de telechargement emis pour un achat. Le jeton envoye au client ne contient que l'identifiant du lien et une
 * signature ; l'expiration et le compteur de telechargements vivent ici, donc le serveur peut les faire respecter
 * (un jeton autoporteur ne saurait pas compter).
 */
@Entity
@Table(name = "purchase_link")
public class PurchaseLink implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @NotNull
    @Min(1)
    @Column(name = "max_downloads", nullable = false)
    private Integer maxDownloads;

    @NotNull
    @Min(0)
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Purchase purchase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxDownloads() {
        return maxDownloads;
    }

    public void setMaxDownloads(Integer maxDownloads) {
        this.maxDownloads = maxDownloads;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PurchaseLink)) {
            return false;
        }
        return getId() != null && getId().equals(((PurchaseLink) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PurchaseLink{id=" + id + ", expiresAt=" + expiresAt + ", downloadCount=" + downloadCount + "}";
    }
}
