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
    long countByStatus(com.afrivox.app.domain.enumeration.RecordingStatus status);

    /** Duree totale des enregistrements valides, en millisecondes. */
    @Query(
        "select coalesce(sum(r.durationMs), 0) from Recording r " +
            "where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED"
    )
    long validatedDurationMs();

    /** Contributeurs distincts ayant envoye au moins un enregistrement actif (envoye ou valide). */
    @Query(
        "select count(distinct r.contributor.id) from Recording r where r.status in " +
            "(com.afrivox.app.domain.enumeration.RecordingStatus.SUBMITTED, com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED)"
    )
    long countActiveContributors();

    /** Enregistrements valides d'une langue (via les campagnes de cette langue). */
    @Query(
        "select count(r) from Recording r where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED " +
            "and r.prompt.campaign.language.id = :languageId"
    )
    long countValidatedByLanguage(@Param("languageId") Long languageId);

    @Query(
        "select coalesce(sum(r.durationMs), 0) from Recording r where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED " +
            "and r.prompt.campaign.language.id = :languageId"
    )
    long validatedDurationMsByLanguage(@Param("languageId") Long languageId);

    /**
     * Comme {@link #countValidatedByLanguage} mais restreint au domaine des campagnes ({@code domain} en minuscules ;
     * chaine vide = tous les domaines). Sert aux chiffres affiches pour un dataset qui a un domaine.
     */
    @Query(
        "select count(r) from Recording r where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED " +
            "and r.prompt.campaign.language.id = :languageId " +
            "and (:domain = '' or lower(r.prompt.campaign.domain) = :domain)"
    )
    long countValidatedByLanguageAndDomain(@Param("languageId") Long languageId, @Param("domain") String domain);

    @Query(
        "select coalesce(sum(r.durationMs), 0) from Recording r where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED " +
            "and r.prompt.campaign.language.id = :languageId " +
            "and (:domain = '' or lower(r.prompt.campaign.domain) = :domain)"
    )
    long validatedDurationMsByLanguageAndDomain(@Param("languageId") Long languageId, @Param("domain") String domain);

    /**
     * Enregistrements eligibles a un dataset : valides, de la langue demandee, du domaine demande (chaine vide = tous
     * les domaines), et dont le contributeur a encore un consentement de collecte actif (un retrait de consentement
     * exclut donc ses audios des versions suivantes).
     */
    @Query(
        "select r from Recording r join fetch r.prompt p join fetch r.contributor where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED " +
            "and r.prompt.campaign.language.id = :languageId and (:domain = '' or lower(r.prompt.campaign.domain) = :domain) " +
            "and exists (select c.id from Consent c where c.profile.id = r.contributor.id " +
            "and c.kind = com.afrivox.app.domain.enumeration.ConsentKind.COLLECTION and c.revokedAt is null) order by r.id"
    )
    List<Recording> findEligibleForDataset(@Param("languageId") Long languageId, @Param("domain") String domain);

    /** Enregistrements a relire : envoyes, de la langue, pas les miens, pas deja relus par moi, pas encore assez relus. */
    @Query(
        "select r from Recording r join fetch r.prompt where r.status = com.afrivox.app.domain.enumeration.RecordingStatus.SUBMITTED " +
            "and r.prompt.campaign.language.id = :languageId and r.contributor.id <> :reviewerId " +
            "and not exists (select rv.id from RecordingReview rv where rv.recording.id = r.id and rv.reviewer.id = :reviewerId) " +
            "and (select count(rv2) from RecordingReview rv2 where rv2.recording.id = r.id and rv2.gold = false) < :maxReviews " +
            "order by r.createdAt"
    )
    List<Recording> findToReview(
        @Param("languageId") Long languageId,
        @Param("reviewerId") Long reviewerId,
        @Param("maxReviews") long maxReviews,
        Pageable pageable
    );

    /** Enregistrements deja tranches (candidats aux taches pieges) que ce relecteur n'a jamais vus. */
    @Query(
        "select r from Recording r join fetch r.prompt where r.status in (com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED, " +
            "com.afrivox.app.domain.enumeration.RecordingStatus.REJECTED) and r.prompt.campaign.language.id = :languageId " +
            "and r.contributor.id <> :reviewerId " +
            "and not exists (select rv.id from RecordingReview rv where rv.recording.id = r.id and rv.reviewer.id = :reviewerId) " +
            "order by r.id"
    )
    List<Recording> findSettledForTrap(@Param("languageId") Long languageId, @Param("reviewerId") Long reviewerId, Pageable pageable);

    /** Enregistrements d'un contributeur (les siens). */
    Page<Recording> findAllByContributorId(Long contributorId, Pageable pageable);

    /** Nombre de phrases d'une campagne deja enregistrees (envoyees ou validees) par ce contributeur. */
    @Query(
        "select count(distinct recording.prompt.id) from Recording recording where recording.contributor.id = :profileId " +
            "and recording.prompt.campaign.id = :campaignId and recording.status in " +
            "(com.afrivox.app.domain.enumeration.RecordingStatus.SUBMITTED, com.afrivox.app.domain.enumeration.RecordingStatus.VALIDATED)"
    )
    long countRecordedPrompts(@Param("profileId") Long profileId, @Param("campaignId") Long campaignId);

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

    /** Enregistrements par langue et par statut : {@code [languageId, status, count]} (une requête, pas de N+1). */
    @Query(
        "select r.prompt.campaign.language.id, r.status, count(r) from Recording r " +
            "where r.prompt.campaign.language is not null group by r.prompt.campaign.language.id, r.status"
    )
    List<Object[]> countByLanguageAndStatus();
}
