package com.bank.feature.exchange.web;

import com.bank.feature.exchange.domain.ExchangeService;
import com.bank.feature.exchange.web.dto.ExchangeQuoteView;
import com.bank.feature.exchange.web.dto.ExchangeRateView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Exchange", description = "Currency exchange rates, quotes, and conversions")
@RestController
@RequestMapping("/v1/exchange")
public class ExchangeController {

    public record QuoteRequest(@NotBlank String fromCurrency, @NotBlank String toCurrency,
                               @NotNull @Positive BigDecimal fromAmount) {}

    public record ExecuteRequest(@NotNull UUID quoteId,
                                 @NotNull UUID fromWalletId, @NotNull UUID toWalletId) {}

    private final ExchangeService exchange;

    public ExchangeController(ExchangeService exchange) {
        this.exchange = exchange;
    }

    @Operation(summary = "List all supported currencies (public)")
    @GetMapping("/currencies")
    public List<String> currencies() {
        return exchange.supportedCurrencies();
    }

    @Operation(summary = "Current rate for a currency pair (public)")
    @GetMapping("/rates")
    public ExchangeRateView rates(@RequestParam String from, @RequestParam String to) {
        return exchange.currentRate(from, to);
    }

    @Operation(summary = "Get a conversion quote with fee")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/quote")
    @PreAuthorize("hasAuthority('wallet:read')")
    public ExchangeQuoteView quote(@RequestBody QuoteRequest req) {
        return exchange.quote(req.fromCurrency(), req.toCurrency(), req.fromAmount());
    }

    @Operation(summary = "Execute a conversion")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void execute(@RequestBody ExecuteRequest req) {
        exchange.execute(req.quoteId(), req.fromWalletId(), req.toWalletId());
    }
}
