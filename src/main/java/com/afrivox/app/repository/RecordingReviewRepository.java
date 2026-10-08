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
    List<RecordingReview> findAllByRecordingId(Long recordingId);

    boolean existsByRecordingIdAndReviewerId(Long recordingId, Long reviewerId);

    /**
     * Fiabilite d'un relecteur sur une langue : {@code [nombre de taches pieges, nombre de bonnes reponses]}.
     * Une reponse est bonne si elle va dans le sens de la decision deja prise (valide = ACCEPT, rejete = REJECT).
     */
    @Query(
        "select count(rv), coalesce(sum(case when (rv.verdict = com.afrivox.app.domain.enumeration.Verdict.ACCEPT " +
            "and r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED) or (rv.verdict = com.afrivox.app.domain.enumeration.Verdict.REJECT " +
            "and r.status = com.afrivox.app.domain.enumeration.RecordingStatus.REJECTED) then 1 else 0 end), 0) " +
            "from RecordingReview rv join rv.recording r where rv.gold = true and rv.reviewer.id = :reviewerId " +
            "and r.prompt.campaign.language.id = :languageId"
    )
    List<Object[]> trapStats(@Param("reviewerId") Long reviewerId, @Param("languageId") Long languageId);

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
