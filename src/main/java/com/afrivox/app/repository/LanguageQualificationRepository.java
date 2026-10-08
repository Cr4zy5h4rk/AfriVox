package com.afrivox.app.repository;

import com.afrivox.app.domain.LanguageQualification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the LanguageQualification entity.
 */
@Repository
public interface LanguageQualificationRepository extends JpaRepository<LanguageQualification, Long> {
    default Optional<LanguageQualification> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<LanguageQualification> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<LanguageQualification> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select languageQualification from LanguageQualification languageQualification left join fetch languageQualification.profile left join fetch languageQualification.language",
        countQuery = "select count(languageQualification) from LanguageQualification languageQualification"
    )
    Page<LanguageQualification> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select languageQualification from LanguageQualification languageQualification left join fetch languageQualification.profile left join fetch languageQualification.language"
    )
    List<LanguageQualification> findAllWithToOneRelationships();

    @Query(
        "select languageQualification from LanguageQualification languageQualification left join fetch languageQualification.profile left join fetch languageQualification.language where languageQualification.id =:id"
    )
    Optional<LanguageQualification> findOneWithToOneRelationships(@Param("id") Long id);

    /** Nombre de qualifications par langue pour un statut donné : {@code [languageId, count]} (une requête, pas de N+1). */
    @Query(
        "select q.language.id, count(q) from LanguageQualification q where q.status = :status and q.language is not null group by q.language.id"
    )
    List<Object[]> countByLanguageForStatus(@Param("status") com.afrivox.app.domain.enumeration.QualificationStatus status);
}
