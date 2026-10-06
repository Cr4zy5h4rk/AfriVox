package com.afrivox.app.repository;

import com.afrivox.app.domain.RecordingReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RecordingReview entity.
 */
@Repository
public interface RecordingReviewRepository extends JpaRepository<RecordingReview, Long> {
    default Optional<RecordingReview> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RecordingReview> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RecordingReview> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recordingReview from RecordingReview recordingReview left join fetch recordingReview.reviewer",
        countQuery = "select count(recordingReview) from RecordingReview recordingReview"
    )
    Page<RecordingReview> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recordingReview from RecordingReview recordingReview left join fetch recordingReview.reviewer")
    List<RecordingReview> findAllWithToOneRelationships();

    @Query(
        "select recordingReview from RecordingReview recordingReview left join fetch recordingReview.reviewer where recordingReview.id =:id"
    )
    Optional<RecordingReview> findOneWithToOneRelationships(@Param("id") Long id);
}
