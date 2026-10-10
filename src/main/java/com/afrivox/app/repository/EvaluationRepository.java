package com.afrivox.app.repository;

import com.afrivox.app.domain.Evaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Evaluation entity.
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    /** Evaluations d'un modele, meilleur taux d'erreur (WER) d'abord. */
    @Query("select e from Evaluation e where e.model.id = :modelId order by e.wer asc nulls last")
    List<Evaluation> findForModel(@Param("modelId") Long modelId);

    /** Meilleur WER d'un modele (vide s'il n'a aucune evaluation). */
    @Query("select min(e.wer) from Evaluation e where e.model.id = :modelId")
    Float bestWer(@Param("modelId") Long modelId);

    /** Derniere evaluation d'un modele sur un jeu de test (pour limiter la frequence des envois). */
    Optional<Evaluation> findFirstByModelIdAndTestSetIdOrderBySubmittedAtDesc(Long modelId, Long testSetId);

    /**
     * Classement : la meilleure evaluation de chaque modele (une seule ligne par modele, a egalite la plus ancienne),
     * meilleures d'abord, filtrable par langue ('' = toutes). L'historique complet est sur la fiche du modele.
     */
    @Query(
        "select e from Evaluation e join fetch e.model m left join fetch m.language l " +
            "where e.wer is not null and (:lang = '' or l.code = :lang) " +
            "and e.id = (select min(e2.id) from Evaluation e2 where e2.model = e.model " +
            "and e2.wer = (select min(e3.wer) from Evaluation e3 where e3.model = e.model)) " +
            "order by e.wer asc, e.submittedAt asc"
    )
    List<Evaluation> leaderboard(@Param("lang") String languageCode, Pageable pageable);

    default Optional<Evaluation> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Evaluation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Evaluation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select evaluation from Evaluation evaluation left join fetch evaluation.model left join fetch evaluation.testSet",
        countQuery = "select count(evaluation) from Evaluation evaluation"
    )
    Page<Evaluation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select evaluation from Evaluation evaluation left join fetch evaluation.model left join fetch evaluation.testSet")
    List<Evaluation> findAllWithToOneRelationships();

    @Query(
        "select evaluation from Evaluation evaluation left join fetch evaluation.model left join fetch evaluation.testSet where evaluation.id =:id"
    )
    Optional<Evaluation> findOneWithToOneRelationships(@Param("id") Long id);
}
