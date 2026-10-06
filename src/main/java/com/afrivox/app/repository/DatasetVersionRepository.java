package com.afrivox.app.repository;

import com.afrivox.app.domain.DatasetVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DatasetVersion entity.
 *
 * When extending this class, extend DatasetVersionRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface DatasetVersionRepository extends DatasetVersionRepositoryWithBagRelationships, JpaRepository<DatasetVersion, Long> {
    default Optional<DatasetVersion> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<DatasetVersion> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<DatasetVersion> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select datasetVersion from DatasetVersion datasetVersion left join fetch datasetVersion.dataset",
        countQuery = "select count(datasetVersion) from DatasetVersion datasetVersion"
    )
    Page<DatasetVersion> findAllWithToOneRelationships(Pageable pageable);

    @Query("select datasetVersion from DatasetVersion datasetVersion left join fetch datasetVersion.dataset")
    List<DatasetVersion> findAllWithToOneRelationships();

    @Query("select datasetVersion from DatasetVersion datasetVersion left join fetch datasetVersion.dataset where datasetVersion.id =:id")
    Optional<DatasetVersion> findOneWithToOneRelationships(@Param("id") Long id);
}
