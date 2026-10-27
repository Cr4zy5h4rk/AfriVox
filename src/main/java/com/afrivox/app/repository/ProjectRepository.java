package com.afrivox.app.repository;

import com.afrivox.app.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    /**
     * Recherche publique : texte (titre, resume), langue et etiquette. L'etiquette se cherche entre virgules pour ne pas
     * confondre « asr » et « asr-wolof ». Les motifs sont echappes par le service avec un antislash : la clause
     * {@code escape} est donc explicite (sans elle, un tiret bas saisi ne correspond pas a lui-meme).
     */
    @Query(
        value = "select p from Project p join fetch p.owner o left join fetch p.model m " +
            "where (:q is null or lower(p.title) like :q escape '\\' or lower(p.summary) like :q escape '\\') " +
            "and (:language is null or p.languageCode = :language) " +
            "and (:tag is null or concat(',', coalesce(p.tags, ''), ',') like :tag escape '\\')",
        countQuery = "select count(p) from Project p " +
            "where (:q is null or lower(p.title) like :q escape '\\' or lower(p.summary) like :q escape '\\') " +
            "and (:language is null or p.languageCode = :language) " +
            "and (:tag is null or concat(',', coalesce(p.tags, ''), ',') like :tag escape '\\')"
    )
    Page<Project> search(@Param("q") String q, @Param("language") String language, @Param("tag") String tag, Pageable pageable);

    @Query("select p from Project p join fetch p.owner o left join fetch p.model m where p.id = :id")
    Optional<Project> findWithOwner(@Param("id") Long id);

    @Query(
        "select p from Project p join fetch p.owner o left join fetch p.model m where p.owner.id = :ownerId order by p.createdAt desc, p.id desc"
    )
    List<Project> findByOwner(@Param("ownerId") Long ownerId);

    long countByOwnerId(Long ownerId);
}
