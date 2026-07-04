package com.bank.feature.limits.domain;

import com.bank.feature.limits.persistence.LimitUsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * FR-20.4: limit usage resets automatically. Usage is bucketed by period key, so
 * a new day/month already starts at zero; this hourly job reclaims stale rows
 * from prior periods so the table does not grow unbounded.
 */
@Component
public class LimitMaintenance {

    private static final Logger log = LoggerFactory.getLogger(LimitMaintenance.class);

    private final LimitUsageRepository usages;

    public LimitMaintenance(LimitUsageRepository usages) {
        this.usages = usages;
    }

    @Scheduled(fixedDelay = 3_600_000)   // hourly
    @Transactional
    public void purgeStaleUsage() {
        LocalDate today = LocalDate.now();
        int daily = usages.deleteStale("DAILY", today.toString());
        int monthly = usages.deleteStale("MONTHLY", today.withDayOfMonth(1).toString());
        if (daily + monthly > 0) {
            log.info("[LIMITS] Purged {} daily and {} monthly stale usage rows", daily, monthly);
        }
    }
}
