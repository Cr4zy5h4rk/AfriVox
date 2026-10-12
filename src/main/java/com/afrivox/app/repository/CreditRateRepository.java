package com.afrivox.app.repository;

import com.afrivox.app.domain.CreditRate;
import com.afrivox.app.domain.enumeration.CreditRateKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditRateRepository extends JpaRepository<CreditRate, CreditRateKind> {}
