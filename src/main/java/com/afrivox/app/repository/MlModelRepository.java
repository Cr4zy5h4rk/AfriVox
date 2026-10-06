package com.afrivox.app.repository;

import com.afrivox.app.domain.MlModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MlModel entity.
 */
@Repository
public interface MlModelRepository extends JpaRepository<MlModel, Long> {
    default Optional<MlModel> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MlModel> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MlModel> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select mlModel from MlModel mlModel left join fetch mlModel.owner left join fetch mlModel.language left join fetch mlModel.trainedOn",
        countQuery = "select count(mlModel) from MlModel mlModel"
    )
    Page<MlModel> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select mlModel from MlModel mlModel left join fetch mlModel.owner left join fetch mlModel.language left join fetch mlModel.trainedOn"
    )
    List<MlModel> findAllWithToOneRelationships();

    @Query(
        "select mlModel from MlModel mlModel left join fetch mlModel.owner left join fetch mlModel.language left join fetch mlModel.trainedOn where mlModel.id =:id"
    )
    Optional<MlModel> findOneWithToOneRelationships(@Param("id") Long id);
}
