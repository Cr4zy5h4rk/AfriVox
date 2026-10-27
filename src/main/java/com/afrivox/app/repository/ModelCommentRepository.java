package com.afrivox.app.repository;

import com.afrivox.app.domain.ModelComment;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelCommentRepository extends JpaRepository<ModelComment, Long> {
    /** Commentaires de premier niveau d'un modele, du plus recent au plus ancien. */
    @Query(
        value = "select c from ModelComment c join fetch c.author a left join fetch a.user " +
            "where c.modelId = :modelId and c.parentId is null order by c.createdAt desc, c.id desc",
        countQuery = "select count(c) from ModelComment c where c.modelId = :modelId and c.parentId is null"
    )
    Page<ModelComment> findForModel(@Param("modelId") Long modelId, Pageable pageable);

    /** Reponses des commentaires donnes, de la plus ancienne a la plus recente. */
    @Query(
        "select c from ModelComment c join fetch c.author a left join fetch a.user " +
            "where c.parentId in :parentIds order by c.createdAt asc, c.id asc"
    )
    List<ModelComment> findReplies(@Param("parentIds") Collection<Long> parentIds);

    /** Commentaires de premier niveau d'un projet, du plus recent au plus ancien. */
    @Query(
        value = "select c from ModelComment c join fetch c.author a left join fetch a.user " +
            "where c.projectId = :projectId and c.parentId is null order by c.createdAt desc, c.id desc",
        countQuery = "select count(c) from ModelComment c where c.projectId = :projectId and c.parentId is null"
    )
    Page<ModelComment> findForProject(@Param("projectId") Long projectId, Pageable pageable);

    /** Tous les commentaires d'un projet, reponses comprises. */
    long countByProjectId(Long projectId);

    /** Tous les commentaires d'un modele, reponses comprises. */
    long countByModelId(Long modelId);

    /** Pour limiter le rythme : commentaires d'un auteur depuis un instant donne. */
    long countByAuthorIdAndCreatedAtAfter(Long authorId, Instant since);
}
