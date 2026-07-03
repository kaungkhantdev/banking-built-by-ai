package com.bank.feature.exchange.web.dto;

import com.bank.feature.exchange.persistence.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateView(
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        Instant effectiveAt
) {
    public static ExchangeRateView of(ExchangeRate r) {
        return new ExchangeRateView(r.getFromCurrency(), r.getToCurrency(),
                r.getRate(), r.getEffectiveAt());
    }
}
