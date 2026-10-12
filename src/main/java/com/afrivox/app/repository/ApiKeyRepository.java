package com.afrivox.app.repository;

import com.afrivox.app.domain.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ApiKey entity.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    default Optional<ApiKey> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ApiKey> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ApiKey> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select apiKey from ApiKey apiKey left join fetch apiKey.owner", countQuery = "select count(apiKey) from ApiKey apiKey")
    Page<ApiKey> findAllWithToOneRelationships(Pageable pageable);

    @Query("select apiKey from ApiKey apiKey left join fetch apiKey.owner")
    List<ApiKey> findAllWithToOneRelationships();

    @Query("select apiKey from ApiKey apiKey left join fetch apiKey.owner where apiKey.id =:id")
    Optional<ApiKey> findOneWithToOneRelationships(@Param("id") Long id);

    /** Authentification : la cle et son proprietaire (avec son compte et ses roles) en une requete. */
    @Query("select k from ApiKey k join fetch k.owner o join fetch o.user u left join fetch u.authorities where k.prefix = :prefix")
    Optional<ApiKey> findOneForAuthentication(@Param("prefix") String prefix);

    boolean existsByPrefix(String prefix);

    List<ApiKey> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<ApiKey> findOneByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerIdAndRevokedAtIsNull(Long ownerId);
}
