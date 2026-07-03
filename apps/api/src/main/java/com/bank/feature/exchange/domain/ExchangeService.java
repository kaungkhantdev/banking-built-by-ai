package com.bank.feature.exchange.domain;

import com.bank.feature.exchange.web.dto.ExchangeQuoteView;
import com.bank.feature.exchange.web.dto.ExchangeRateView;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExchangeService {

    List<String> supportedCurrencies();

    ExchangeRateView currentRate(String fromCurrency, String toCurrency);

    ExchangeQuoteView quote(String fromCurrency, String toCurrency, BigDecimal fromAmount);

    void execute(UUID quoteId, UUID fromWalletId, UUID toWalletId);
}
