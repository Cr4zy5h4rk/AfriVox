package com.afrivox.app.repository;

import com.afrivox.app.domain.Recording;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Recording entity.
 */
@Repository
public interface RecordingRepository extends JpaRepository<Recording, Long> {
    /** Enregistrements d'un contributeur (les siens). */
    Page<Recording> findAllByContributorId(Long contributorId, Pageable pageable);

    /** Detection de doublon exact : meme contenu deja envoye par ce contributeur. */
    boolean existsByContributorIdAndSha256(Long contributorId, String sha256);

    default Optional<Recording> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Recording> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Recording> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select recording from Recording recording left join fetch recording.contributor",
        countQuery = "select count(recording) from Recording recording"
    )
    Page<Recording> findAllWithToOneRelationships(Pageable pageable);

    @Query("select recording from Recording recording left join fetch recording.contributor")
    List<Recording> findAllWithToOneRelationships();

    @Query("select recording from Recording recording left join fetch recording.contributor where recording.id =:id")
    Optional<Recording> findOneWithToOneRelationships(@Param("id") Long id);
}
