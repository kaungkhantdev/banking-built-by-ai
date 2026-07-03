package com.bank.feature.exchange.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExchangeQuoteView(
        UUID quoteId,
        String fromCurrency,
        String toCurrency,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        BigDecimal rate,
        BigDecimal feeAmount,
        Instant expiresAt
) {}
