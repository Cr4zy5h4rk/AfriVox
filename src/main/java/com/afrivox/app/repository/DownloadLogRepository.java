package com.afrivox.app.repository;

import com.afrivox.app.domain.DownloadLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DownloadLog entity.
 */
@Repository
public interface DownloadLogRepository extends JpaRepository<DownloadLog, Long> {
    default Optional<DownloadLog> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<DownloadLog> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<DownloadLog> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select downloadLog from DownloadLog downloadLog left join fetch downloadLog.datasetVersion left join fetch downloadLog.profile",
        countQuery = "select count(downloadLog) from DownloadLog downloadLog"
    )
    Page<DownloadLog> findAllWithToOneRelationships(Pageable pageable);

    @Query("select downloadLog from DownloadLog downloadLog left join fetch downloadLog.datasetVersion left join fetch downloadLog.profile")
    List<DownloadLog> findAllWithToOneRelationships();

    @Query(
        "select downloadLog from DownloadLog downloadLog left join fetch downloadLog.datasetVersion left join fetch downloadLog.profile where downloadLog.id =:id"
    )
    Optional<DownloadLog> findOneWithToOneRelationships(@Param("id") Long id);
}
