package com.bank.feature.fees.domain;

import com.bank.feature.fees.persistence.FeeConfig;
import com.bank.feature.fees.persistence.FeeConfigRepository;
import com.bank.shared.utils.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Pure domain service — no HTTP layer. Called by DefaultTransferService. */
@Service
public class FeeEngine {

    private final FeeConfigRepository configs;

    public FeeEngine(FeeConfigRepository configs) {
        this.configs = configs;
    }

    public FeeResult calculate(BigDecimal amount, String transferType, String customerTier) {
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
