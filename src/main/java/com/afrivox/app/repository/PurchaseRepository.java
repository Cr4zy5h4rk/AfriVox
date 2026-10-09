package com.afrivox.app.repository;

import com.afrivox.app.domain.Purchase;
import com.afrivox.app.domain.enumeration.LicenseKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    /** Achats d'un compte, recents d'abord, avec version et dataset charges (evite les requetes une par une). */
    @Query(
        "select p from Purchase p join fetch p.datasetVersion v join fetch v.dataset " +
            "where p.profile.id = :profileId order by p.createdAt desc"
    )
    List<Purchase> findAllByProfile(@Param("profileId") Long profileId);

    /** Un achat, seulement s'il appartient au compte : un achat d'autrui est indistinguable d'un achat inexistant. */
    @Query(
        "select p from Purchase p join fetch p.datasetVersion v join fetch v.dataset " + "where p.id = :id and p.profile.id = :profileId"
    )
    Optional<Purchase> findOneByIdAndProfile(@Param("id") Long id, @Param("profileId") Long profileId);

    boolean existsByProfileIdAndDatasetVersionIdAndLicense(Long profileId, Long datasetVersionId, LicenseKind license);
}
