package com.bank.feature.fees.domain;

import com.bank.feature.fees.persistence.FeeConfig;
import com.bank.feature.fees.persistence.FeeConfigRepository;
import com.bank.feature.fees.persistence.WaiverRecordRepository;
import com.bank.shared.utils.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Pure domain service — no HTTP layer. Called by DefaultTransferService. */
@Service
public class FeeEngine {

    private final FeeConfigRepository configs;
    private final WaiverRecordRepository waivers;

    public FeeEngine(FeeConfigRepository configs, WaiverRecordRepository waivers) {
        this.configs = configs;
        this.waivers = waivers;
    }

    /**
     * @param payerId the sender's account (fee/waiver scope); may be {@code null}
     *                for callers with no principal (fees still apply, no waiver)
     */
    public FeeResult calculate(UUID payerId, BigDecimal amount,
                               String transferType, String customerTier) {
        // FR-19.4: an active promotional waiver zeroes the fee.
        if (payerId != null && waivers.hasActiveWaiver(payerId, Instant.now())) {
            return FeeResult.noFee(Money.of(amount));
        }

        Optional<FeeConfig> cfg = configs.findByTransferTypeAndCustomerTierAndActiveTrue(
                transferType, customerTier);

        if (cfg.isEmpty()) {
            return FeeResult.noFee(Money.of(amount));
        }

        FeeConfig c = cfg.get();
        BigDecimal fee;
        if ("PERCENTAGE".equals(c.getFeeType())) {
            fee = amount.multiply(c.getFeeValue()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        } else {
            fee = c.getFeeValue();
        }

        if (c.getMinFee() != null && fee.compareTo(c.getMinFee()) < 0) fee = c.getMinFee();
        if (c.getMaxFee() != null && fee.compareTo(c.getMaxFee()) > 0) fee = c.getMaxFee();

        return new FeeResult(Money.of(amount), Money.of(fee));
    }
}
