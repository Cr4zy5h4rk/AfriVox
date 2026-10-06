package com.afrivox.app.repository;

import com.afrivox.app.domain.Campaign;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Campaign entity.
 */
@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    default Optional<Campaign> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Campaign> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Campaign> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select campaign from Campaign campaign left join fetch campaign.language",
        countQuery = "select count(campaign) from Campaign campaign"
    )
    Page<Campaign> findAllWithToOneRelationships(Pageable pageable);

    @Query("select campaign from Campaign campaign left join fetch campaign.language")
    List<Campaign> findAllWithToOneRelationships();

    @Query("select campaign from Campaign campaign left join fetch campaign.language where campaign.id =:id")
    Optional<Campaign> findOneWithToOneRelationships(@Param("id") Long id);
}
