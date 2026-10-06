package com.afrivox.app.repository;

import com.afrivox.app.domain.Consent;
import com.afrivox.app.domain.enumeration.ConsentKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Consent entity.
 */
@Repository
public interface ConsentRepository extends JpaRepository<Consent, Long> {
    /** Consentements d'un profil, du plus recent au plus ancien. */
    List<Consent> findAllByProfileIdOrderByGrantedAtDesc(Long profileId);

    /** Consentement par id, uniquement s'il appartient au profil (evite de reveler ceux des autres). */
    Optional<Consent> findOneByIdAndProfileId(Long id, Long profileId);

    /** Consentement encore actif (non retire) d'un profil pour un type donne. */
    Optional<Consent> findFirstByProfileIdAndKindAndRevokedAtIsNull(Long profileId, ConsentKind kind);

    default Optional<Consent> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Consent> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Consent> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select consent from Consent consent left join fetch consent.profile",
        countQuery = "select count(consent) from Consent consent"
    )
    Page<Consent> findAllWithToOneRelationships(Pageable pageable);

    @Query("select consent from Consent consent left join fetch consent.profile")
    List<Consent> findAllWithToOneRelationships();

    @Query("select consent from Consent consent left join fetch consent.profile where consent.id =:id")
    Optional<Consent> findOneWithToOneRelationships(@Param("id") Long id);
}
