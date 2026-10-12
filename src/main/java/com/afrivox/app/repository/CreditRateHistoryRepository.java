package com.afrivox.app.repository;

import com.afrivox.app.domain.CreditRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditRateHistoryRepository extends JpaRepository<CreditRateHistory, Long> {}
