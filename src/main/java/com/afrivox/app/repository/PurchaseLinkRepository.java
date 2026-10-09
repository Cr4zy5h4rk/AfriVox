package com.afrivox.app.repository;

import com.afrivox.app.domain.PurchaseLink;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseLinkRepository extends JpaRepository<PurchaseLink, Long> {
    /** Liens emis pour un achat depuis un instant donne (limite quotidienne de regeneration). */
    long countByPurchaseIdAndCreatedAtAfter(Long purchaseId, Instant since);

    /**
     * Consomme un telechargement de facon atomique : la condition est dans l'UPDATE, donc deux requetes simultanees
     * ne peuvent pas depasser la limite. Renvoie 0 si le lien est expire ou epuise.
     */
    @Modifying(clearAutomatically = true)
    @Query(
        "update PurchaseLink l set l.downloadCount = l.downloadCount + 1 " +
            "where l.id = :id and l.downloadCount < l.maxDownloads and l.expiresAt > :now"
    )
    int consumeDownload(@Param("id") Long id, @Param("now") Instant now);
}
