package com.afrivox.app.repository;

import com.afrivox.app.domain.CreditTransaction;
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
