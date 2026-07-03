# Feature 18 — Currency & Exchange

**Package:** `com.bank.feature.exchange` · **Endpoints:** `/v1/exchange/*`

## Purpose
Import and store exchange rates, calculate conversion amounts and fees, and execute
atomic wallet-to-wallet conversions across different currencies.

## Layout
```
exchange/
├── web/        ExchangeController, dto/ (ExchangeQuoteRequest, ExchangeQuoteView, ExchangeRequest)
├── domain/     ExchangeService + DefaultExchangeService, RateProvider
└── persistence/ ExchangeRate, ExchangeRateRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/exchange/rates` | public | Current rates for a currency pair |
| POST | `/v1/exchange/quote` | `wallet:read` | Get a conversion quote with fee |
| POST | `/v1/exchange` | `transfer:create` | Execute a conversion |

## Key rules
- Rates are imported automatically on a schedule (e.g., from an external FX provider)
  and stored with their effective timestamp; historical rates are never overwritten.
- A quote locks a rate for a short TTL (e.g., 30 s); the execute step validates
  the quote has not expired before proceeding.
- The conversion is atomic: debit the source wallet, credit the destination wallet,
  and record the fee — all in one `@Transactional` unit (mirrors Feature 6).
- Amounts are computed using `NUMERIC(19,4)` with explicit rounding rules (e.g.,
  `HALF_UP`) to prevent float drift.

## Why
Storing historical rates allows exact reconstruction of what rate was applied to any
historical conversion, which is required for financial audits.

## Related requirements
FR-18.*, FR-5.9
