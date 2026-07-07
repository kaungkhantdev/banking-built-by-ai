package com.bank.feature.fraud.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {

    @Query("select count(a) from FraudAlert a where a.userId = :userId and a.createdAt > :since")
    long countRecentByUserId(@Param("userId") UUID userId, @Param("since") Instant since);

    Page<FraudAlert> findByStatus(String status, Pageable pageable);
}
