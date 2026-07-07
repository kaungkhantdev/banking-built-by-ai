package com.bank.feature.exchange.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seam for an upstream FX rate feed (production: a real provider API; default:
 * {@link StubRateProvider}). Mirrors the {@code KycVendorClient} pattern.
 */
public interface RateProvider {

    List<Quote> fetchRates();

    record Quote(String fromCurrency, String toCurrency, BigDecimal rate) {}
}
