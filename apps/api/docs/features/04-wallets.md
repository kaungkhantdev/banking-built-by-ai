# Feature 4 — Wallets

**Package:** `com.bank.feature.wallets` · **Endpoints:** `/v1/wallets/*`

## Purpose
A **Wallet** is a currency-scoped balance container under an account. Its balance
is **never stored** — it is derived by summing ledger entries.

## Layout
```
wallets/
├── web/        WalletController (OpenWalletRequest), dto/ (WalletView, BalanceView)
├── domain/     WalletService + DefaultWalletService
└── persistence/ Wallet, WalletStatus, WalletRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/wallets` | `wallet:create` | Open a currency wallet |
| GET  | `/v1/wallets/{id}/balance` | `wallet:read` | Derived balance |
| POST | `/v1/wallets/{id}/freeze` | `wallet:manage` | ACTIVE → FROZEN |

## Key rules
- **One wallet per (account, currency)** — DB unique constraint
  `uq_wallet_account_ccy`.
- **Derived balance:** `DefaultWalletService.balance` delegates to
  `LedgerService.balanceOf`, which is `SUM(CREDIT) - SUM(DEBIT)` over
  `ledger_entries`. No cached column to drift.
- **`getActive(id)`** is the money path's gate: it throws `422 WALLET_NOT_ACTIVE`
  for frozen/closed wallets, so transfers can't touch them.

## Why
A stored balance invites drift from a bug or partial failure. Deriving it from an
append-only ledger makes the balance provably the sum of immutable facts.

## Code pointers
- Derived read: `DefaultWalletService.balance` → `DefaultLedgerService.balanceOf`
- Money-path guard: `DefaultWalletService.getActive`

## Related requirements
FR-4.*, NFR-2.6
