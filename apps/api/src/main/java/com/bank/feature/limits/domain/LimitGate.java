package com.bank.feature.limits.domain;

import com.bank.feature.limits.persistence.LimitConfig;
import com.bank.feature.limits.persistence.LimitConfigRepository;
import com.bank.feature.limits.persistence.LimitUsage;
import com.bank.feature.limits.persistence.LimitUsageRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Enforces per-tier daily and monthly limits. Must run inside the caller's transaction. */
@Service
public class LimitGate {

    private final LimitConfigRepository configs;
    private final LimitUsageRepository usages;

    public LimitGate(LimitConfigRepository configs, LimitUsageRepository usages) {
        this.configs = configs;
        this.usages = usages;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertWithinLimitsAndRecord(UUID userId, BigDecimal amount,
                                             String currency, String customerTier) {
        List<LimitConfig> limits = configs.findByCustomerTierAndActiveTrue(customerTier);
        LocalDate today = LocalDate.now();

        for (LimitConfig cfg : limits) {
            String key = "DAILY".equals(cfg.getPeriod())
                    ? today.toString()
                    : today.withDayOfMonth(1).toString();

            LimitUsage usage = usages.findByUserIdAndPeriodAndPeriodKeyAndCurrency(
                            userId, cfg.getPeriod(), key, currency)
                    .orElseGet(() -> usages.save(
                            new LimitUsage(userId, cfg.getPeriod(), key, currency)));

            if (usage.getUsedAmount().add(amount).compareTo(cfg.getMaxAmount()) > 0) {
                throw new ApiException("LIMIT_EXCEEDED",
                        cfg.getPeriod() + " limit of " + cfg.getMaxAmount() + " " + currency + " exceeded", 422);
            }
            usage.addUsage(amount);
        }
    }
}
