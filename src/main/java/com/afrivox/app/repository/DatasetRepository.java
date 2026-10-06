package com.afrivox.app.repository;

import com.afrivox.app.domain.Dataset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Dataset entity.
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long>, JpaSpecificationExecutor<Dataset> {
    Optional<Dataset> findOneBySlug(String slug);

    default Optional<Dataset> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Dataset> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Dataset> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select dataset from Dataset dataset left join fetch dataset.language",
        countQuery = "select count(dataset) from Dataset dataset"
    )
    Page<Dataset> findAllWithToOneRelationships(Pageable pageable);

    @Query("select dataset from Dataset dataset left join fetch dataset.language")
    List<Dataset> findAllWithToOneRelationships();

    @Query("select dataset from Dataset dataset left join fetch dataset.language where dataset.id =:id")
    Optional<Dataset> findOneWithToOneRelationships(@Param("id") Long id);
}
