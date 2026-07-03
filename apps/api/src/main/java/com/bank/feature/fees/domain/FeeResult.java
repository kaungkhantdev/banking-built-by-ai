package com.bank.feature.fees.domain;

import java.math.BigDecimal;

public record FeeResult(BigDecimal principal, BigDecimal fee) {

    public static FeeResult noFee(BigDecimal principal) {
        return new FeeResult(principal, BigDecimal.ZERO);
    }

    public BigDecimal total() {
        return principal.add(fee);
    }
}
