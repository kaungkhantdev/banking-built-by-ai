package com.bank.feature.limits.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {

    Optional<LimitUsage> findByUserIdAndPeriodAndPeriodKeyAndCurrency(
            UUID userId, String period, String periodKey, String currency);

    /**
     * FR-20.4: purge usage rows from earlier periods. Usage naturally resets when
     * the period key rolls over (a new key starts at zero); this reclaims the
     * now-irrelevant rows for the prior period.
     */
    @Modifying
    @Query("delete from LimitUsage u where u.period = :period and u.periodKey <> :currentKey")
    int deleteStale(@Param("period") String period, @Param("currentKey") String currentKey);
}
