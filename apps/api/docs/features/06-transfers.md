# Feature 6 — Transfers (the money core)

**Package:** `com.bank.feature.transfers` · **Endpoints:** `/v1/transfers/*`

## Purpose
Move money between wallets safely: idempotent, ACID, KYC-gated, with reversals as
compensating entries. This is the most safety-critical feature.

## Layout
```
transfers/
├── web/        TransferController, dto/ (TransferRequest)
├── domain/     TransferService + DefaultTransferService, TransferCommand, TransferResult
└── persistence/ IdempotencyRecord, IdempotencyRepository
```

## Endpoints
| Method | Path | Permission | Notes |
|--------|------|-----------|-------|
| POST | `/v1/transfers` | `transfer:create` | Requires `Idempotency-Key` header; 201 posted / 200 replayed |
| POST | `/v1/transfers/{id}/reverse` | `transaction:reverse` | Optional `X-Reason` header |

## The transfer algorithm (one `@Transactional` unit)
`DefaultTransferService.transfer`:
1. **Validate amount** positive (422 `AMOUNT_INVALID`).
2. **Idempotency insert-first** — save the `idem_key`; a duplicate hits the unique
   constraint → catch `DataIntegrityViolationException` → **replay** the prior
   `transactionId` (200, no double-charge).
3. **Load both wallets ACTIVE**; reject same-wallet (422) and currency mismatch (422).
4. **KYC gate** — `KycGate.assertMoneyOutAllowed(senderAccount)` (403 if not VERIFIED).
5. **Funds check** against the **derived** balance (422 `INSUFFICIENT_FUNDS`).
6. **Post double entry** (DEBIT sender + CREDIT receiver, shared transactionId).
7. **Write outbox row** `transfer.completed` in the *same* transaction.

All seven steps commit together or roll back together.

## Reversal
`reverse(originalTxId, reason)` reads the original legs and posts an
**opposite-direction leg** for each under a new `transactionId`, then emits
`transfer.reversed`. History is never mutated or deleted.

## Why
- One ACID transaction = partial states (debit without credit) can never be seen.
- DB-unique idempotency key = retries after a dropped response are safe.
- Outbox in the same TX = the event is durable exactly when the money is.

## Code pointers
- Core: `DefaultTransferService.transfer` / `.reverse`
- Idempotency record: `IdempotencyRecord` + `uq_idem_key` (V2)
- Test: `DefaultTransferServiceTest` (double-entry, insufficient-funds, replay)

## Related requirements
FR-5.2–5.7, NFR-2.1, NFR-2.2
