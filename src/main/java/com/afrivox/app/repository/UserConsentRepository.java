package com.afrivox.app.repository;

import com.afrivox.app.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link UserConsent} entity.
 */
@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {}
