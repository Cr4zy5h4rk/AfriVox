package com.afrivox.app.repository;

import com.afrivox.app.domain.Language;
import com.afrivox.app.domain.enumeration.LanguageStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Language entity.
 */
@SuppressWarnings("unused")
@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {
    Optional<Language> findOneByCode(String code);

    List<Language> findAllByOrderByNameAsc();

    long countByStatus(LanguageStatus status);
}
