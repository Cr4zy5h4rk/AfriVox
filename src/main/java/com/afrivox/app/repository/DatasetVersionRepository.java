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
    /** Versions d'un dataset dans un etat donne (le catalogue public ne voit que PUBLISHED), plus recentes d'abord. */
    List<DatasetVersion> findAllByDatasetIdAndStatusOrderByCreatedAtDesc(
        Long datasetId,
        com.afrivox.app.domain.enumeration.VersionStatus status
    );

    Optional<com.afrivox.app.domain.DatasetVersion> findOneByDatasetIdAndLabelAndStatus(
        Long datasetId,
        String label,
        com.afrivox.app.domain.enumeration.VersionStatus status
    );

    /**
     * Versions publiees dont le titre ou le slug du dataset correspond au motif LIKE (deja echappe, minuscules), avec
     * leur dataset et sa langue : recherche du formulaire de publication d'un modele.
     */
    @Query(
        "select v from DatasetVersion v join fetch v.dataset d left join fetch d.language " +
            "where v.status = com.afrivox.app.domain.enumeration.VersionStatus.PUBLISHED " +
            "and (lower(d.title) like :pattern escape '\\' or lower(d.slug) like :pattern escape '\\') " +
            "order by d.title, v.createdAt desc"
    )
    List<DatasetVersion> searchPublishedWithDataset(@Param("pattern") String pattern, Pageable pageable);

    boolean existsByDatasetIdAndLabel(Long datasetId, String label);

    /** Nombre de datasets ayant au moins une version publiee. */
    @Query(
        "select count(distinct v.dataset.id) from DatasetVersion v " +
            "where v.status = com.afrivox.app.domain.enumeration.VersionStatus.PUBLISHED"
    )
    long countDatasetsWithPublishedVersion();

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
