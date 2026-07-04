package com.bank.feature.exchange.domain;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default rate feed for local/dev. Emits USD-base pairs with a small random
 * walk each poll so historical rows differ over time. Activate the {@code realfx}
 * profile to supply a real provider instead.
 */
@Component
@Profile("!realfx")
public class StubRateProvider implements RateProvider {

    private record Base(String to, double mid) {}

    private static final List<Base> BASES = List.of(
            new Base("EUR", 0.92), new Base("GBP", 0.79), new Base("JPY", 156.0),
            new Base("THB", 36.5), new Base("SGD", 1.35), new Base("AUD", 1.51),
            new Base("HKD", 7.81), new Base("CNY", 7.24));

    @Override
    public List<Quote> fetchRates() {
        return BASES.stream().map(b -> {
            double jitter = 1 + ThreadLocalRandom.current().nextDouble(-0.002, 0.002);
            BigDecimal rate = BigDecimal.valueOf(b.mid() * jitter).setScale(8, RoundingMode.HALF_UP);
            return new Quote("USD", b.to(), rate);
        }).toList();
    }
}
