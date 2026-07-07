package com.bank.feature.exchange.domain;

import com.bank.feature.exchange.persistence.ExchangeRate;
import com.bank.feature.exchange.persistence.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * FR-18.1: pulls fresh rates from the {@link RateProvider} on a schedule and
 * appends them. Rows are never updated, so history is preserved (FR-18.2).
 */
@Component
public class ExchangeRateImporter {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateImporter.class);

    private final RateProvider provider;
    private final ExchangeRateRepository rates;

    public ExchangeRateImporter(RateProvider provider, ExchangeRateRepository rates) {
        this.provider = provider;
        this.rates = rates;
    }

    /** Runs at startup and hourly thereafter. */
    @Scheduled(initialDelay = 5_000, fixedDelay = 3_600_000)
    @Transactional
    public void importRates() {
        Instant now = Instant.now();
        int count = 0;
        for (RateProvider.Quote q : provider.fetchRates()) {
            rates.save(new ExchangeRate(q.fromCurrency(), q.toCurrency(), q.rate(), now, "import"));
            count++;
        }
        log.info("[FX] Imported {} exchange rates", count);
    }
}
