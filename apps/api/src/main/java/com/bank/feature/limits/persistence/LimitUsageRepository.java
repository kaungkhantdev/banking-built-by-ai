package com.bank.feature.limits.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LimitUsageRepository extends JpaRepository<LimitUsage, UUID> {

    Optional<LimitUsage> findByUserIdAndPeriodAndPeriodKeyAndCurrency(
            UUID userId, String period, String periodKey, String currency);
}
