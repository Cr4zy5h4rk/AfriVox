package com.afrivox.app.repository;

import com.afrivox.app.domain.Prompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Prompt entity.
 */
@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {
    /** Nombre de phrases d'une campagne. */
    long countByCampaignId(Long campaignId);

    /**
     * Phrases d'une campagne que ce contributeur n'a pas encore enregistrees (un envoi rejete ou retire ne compte pas :
     * il peut recommencer). Plusieurs contributeurs peuvent enregistrer la meme phrase : c'est voulu (voix differentes).
     */
    @Query(
        "select prompt from Prompt prompt where prompt.campaign.id = :campaignId and not exists (" +
            "select 1 from Recording recording where recording.prompt = prompt and recording.contributor.id = :profileId " +
            "and recording.status in (com.afrivox.app.domain.enumeration.RecordingStatus.SUBMITTED, " +
            "com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED)) order by prompt.id"
    )
    List<Prompt> findTodo(@Param("campaignId") Long campaignId, @Param("profileId") Long profileId, Pageable pageable);

    default Optional<Prompt> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Prompt> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Prompt> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select prompt from Prompt prompt left join fetch prompt.campaign",
        countQuery = "select count(prompt) from Prompt prompt"
    )
    Page<Prompt> findAllWithToOneRelationships(Pageable pageable);

    @Query("select prompt from Prompt prompt left join fetch prompt.campaign")
    List<Prompt> findAllWithToOneRelationships();

    @Query("select prompt from Prompt prompt left join fetch prompt.campaign where prompt.id =:id")
    Optional<Prompt> findOneWithToOneRelationships(@Param("id") Long id);
}
