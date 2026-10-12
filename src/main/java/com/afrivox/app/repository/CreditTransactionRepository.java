package com.afrivox.app.repository;

import com.afrivox.app.domain.CreditTransaction;
import com.afrivox.app.domain.enumeration.CreditReason;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CreditTransaction entity.
 */
@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    /** Le gain a-t-il deja ete verse ? (le registre ne paie jamais deux fois le meme enregistrement). */
    boolean existsByProfileIdAndReasonAndRecordingId(Long profileId, CreditReason reason, Long recordingId);

    /** Solde = somme du registre ; jamais stocke. */
    @Query("select coalesce(sum(t.amount), 0) from CreditTransaction t where t.profile.id = :profileId")
    long balanceOf(@Param("profileId") Long profileId);

    /** Total gagne par motif (montants positifs seulement), pour le resume du contributeur. */
    @Query(
        "select t.reason, coalesce(sum(t.amount), 0) from CreditTransaction t " +
            "where t.profile.id = :profileId and t.amount > 0 group by t.reason"
    )
    List<Object[]> earnedByReason(@Param("profileId") Long profileId);

    Page<CreditTransaction> findAllByProfileId(Long profileId, Pageable pageable);

    default Optional<CreditTransaction> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CreditTransaction> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CreditTransaction> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select creditTransaction from CreditTransaction creditTransaction left join fetch creditTransaction.profile left join fetch creditTransaction.datasetVersion",
        countQuery = "select count(creditTransaction) from CreditTransaction creditTransaction"
    )
    Page<CreditTransaction> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select creditTransaction from CreditTransaction creditTransaction left join fetch creditTransaction.profile left join fetch creditTransaction.datasetVersion"
    )
    List<CreditTransaction> findAllWithToOneRelationships();

    @Query(
        "select creditTransaction from CreditTransaction creditTransaction left join fetch creditTransaction.profile left join fetch creditTransaction.datasetVersion where creditTransaction.id =:id"
    )
    Optional<CreditTransaction> findOneWithToOneRelationships(@Param("id") Long id);
}
