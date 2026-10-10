package com.afrivox.app.repository;

import com.afrivox.app.domain.DatasetRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DatasetRequest entity.
 */
@Repository
public interface DatasetRequestRepository extends JpaRepository<DatasetRequest, Long>, JpaSpecificationExecutor<DatasetRequest> {
    default Optional<DatasetRequest> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<DatasetRequest> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<DatasetRequest> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select datasetRequest from DatasetRequest datasetRequest left join fetch datasetRequest.language left join fetch datasetRequest.requester",
        countQuery = "select count(datasetRequest) from DatasetRequest datasetRequest"
    )
    Page<DatasetRequest> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select datasetRequest from DatasetRequest datasetRequest left join fetch datasetRequest.language left join fetch datasetRequest.requester"
    )
    List<DatasetRequest> findAllWithToOneRelationships();

    @Query(
        "select datasetRequest from DatasetRequest datasetRequest left join fetch datasetRequest.language left join fetch datasetRequest.requester where datasetRequest.id =:id"
    )
    Optional<DatasetRequest> findOneWithToOneRelationships(@Param("id") Long id);

    /** Demandes d'un demandeur, les plus récentes d'abord (espace organisation). */
    @Query(
        "select datasetRequest from DatasetRequest datasetRequest left join fetch datasetRequest.language where datasetRequest.requester.id = :requesterId order by datasetRequest.createdAt desc"
    )
    List<DatasetRequest> findAllByRequesterId(@Param("requesterId") Long requesterId);
}
