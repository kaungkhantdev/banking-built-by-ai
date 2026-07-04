package com.bank.feature.fees.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface WaiverRecordRepository extends JpaRepository<WaiverRecord, UUID> {

    @Query("""
            select count(w) > 0 from WaiverRecord w
            where w.userId = :userId
              and w.validFrom <= :at
              and w.validUntil >= :at
            """)
    boolean hasActiveWaiver(@Param("userId") UUID userId, @Param("at") Instant at);
}
