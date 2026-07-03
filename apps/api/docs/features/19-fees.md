# Feature 19 — Fees

**Package:** `com.bank.feature.fees` · **Endpoints:** none (internal engine)

## Purpose
Calculate and apply transfer fees before execution, generate separate ledger entries
for fee amounts, and support promotional fee waivers.

## Layout
```
fees/
├── domain/     FeeEngine + DefaultFeeEngine, FeePolicy, FeeWaiver
└── persistence/ FeeConfig, FeeConfigRepository, WaiverRecord, WaiverRepository
```

## How it works
1. `DefaultTransferService` calls `FeeEngine.calculate(command)` before posting any
   ledger entries.
2. `FeeEngine` looks up the applicable `FeeConfig` (by transfer type and customer tier)
   and checks `WaiverRepository` for an active promotional waiver.
3. The returned `FeeResult` carries the principal amount and the fee amount separately.
4. If a fee applies, `DefaultLedgerService` posts two double entries in the same
   transaction: one for the principal, one for the fee (debited from sender,
   credited to a fee collection wallet).

## Key rules
- Fee configs are stored in the database so they can be updated without a redeploy.
- A zero-fee result from a waiver still records a `WaiverRecord` for audit purposes.
- The fee amount is computed using `NUMERIC(19,4)` to prevent rounding drift.
- Fee ledger entries carry a `TRANSACTION_TYPE = FEE` to distinguish them in history.

## Why
Keeping the fee engine as a pure domain service (no HTTP layer) lets it be composed
into transfers, exchanges, and any future money-movement operation without duplication.

## Related requirements
FR-19.*, FR-5.1, FR-5.9
