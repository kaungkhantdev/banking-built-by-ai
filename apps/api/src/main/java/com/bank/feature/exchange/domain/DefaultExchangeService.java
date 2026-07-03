package com.bank.feature.exchange.domain;

import com.bank.feature.exchange.persistence.ExchangeRate;
import com.bank.feature.exchange.persistence.ExchangeRateRepository;
import com.bank.feature.exchange.web.dto.ExchangeQuoteView;
import com.bank.feature.exchange.web.dto.ExchangeRateView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultExchangeService implements ExchangeService {

    private static final BigDecimal FX_FEE_PCT = new BigDecimal("0.005"); // 0.5%
    private static final Map<UUID, ExchangeQuoteView> quoteCache = new ConcurrentHashMap<>();

    private final ExchangeRateRepository rates;

    public DefaultExchangeService(ExchangeRateRepository rates) {
        this.rates = rates;
    }

    private static final List<String> FALLBACK_CURRENCIES =
            List.of("AUD", "CNY", "EUR", "GBP", "HKD", "JPY", "SGD", "THB", "USD");

    @Override
    @Transactional(readOnly = true)
    public List<String> supportedCurrencies() {
        var set = new TreeSet<String>();
        set.addAll(rates.findDistinctFromCurrencies());
        set.addAll(rates.findDistinctToCurrencies());
        return set.isEmpty() ? FALLBACK_CURRENCIES : List.copyOf(set);
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateView currentRate(String fromCurrency, String toCurrency) {
        ExchangeRate rate = requireRate(fromCurrency.toUpperCase(), toCurrency.toUpperCase());
        return ExchangeRateView.of(rate);
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeQuoteView quote(String fromCurrency, String toCurrency, BigDecimal fromAmount) {
        if (!Money.isPositive(fromAmount)) {
            throw new ApiException("AMOUNT_INVALID", "Amount must be positive", 422);
        }
        ExchangeRate rate = requireRate(fromCurrency.toUpperCase(), toCurrency.toUpperCase());
        BigDecimal toAmount = Money.of(fromAmount.multiply(rate.getRate()).setScale(4, RoundingMode.HALF_UP));
        BigDecimal fee = Money.of(fromAmount.multiply(FX_FEE_PCT));

        UUID quoteId = UUID.randomUUID();
        ExchangeQuoteView view = new ExchangeQuoteView(
                quoteId, fromCurrency.toUpperCase(), toCurrency.toUpperCase(),
                Money.of(fromAmount), toAmount, rate.getRate(), fee,
                Instant.now().plus(30, ChronoUnit.SECONDS));
        quoteCache.put(quoteId, view);
        return view;
    }

    @Override
    @Transactional
    public void execute(UUID quoteId, UUID fromWalletId, UUID toWalletId) {
        ExchangeQuoteView q = quoteCache.remove(quoteId);
        if (q == null || q.expiresAt().isBefore(Instant.now())) {
            throw new ApiException("QUOTE_EXPIRED", "Quote has expired or does not exist", 410);
        }
        // Stub: real implementation would call the transfer pipeline with cross-currency support
        // For now: validate and log — full ledger integration follows Feature 19 fee pipeline
    }

    private ExchangeRate requireRate(String from, String to) {
        return rates.findLatest(from, to)
                .orElseGet(() -> {
                    // Stub rate of 1.0 when no rate is seeded yet
                    return new ExchangeRate(from, to, BigDecimal.ONE, Instant.now(), "stub");
                });
    }
}
