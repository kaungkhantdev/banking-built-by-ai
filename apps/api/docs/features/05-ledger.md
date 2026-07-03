# Feature 5 — Ledger

**Package:** `com.bank.feature.ledger` · **Internal** (no public controller)

## Purpose
The double-entry, append-only system of record for money. The single owner and
only writer of `ledger_entries` (Single Responsibility for money correctness).

## Layout
```
ledger/
├── domain/     LedgerService + DefaultLedgerService
└── persistence/ LedgerEntry, Direction (DEBIT|CREDIT), LedgerEntryRepository
```

## Model
- **LedgerEntry** — one leg of a posting: `transactionId`, `walletId`, `direction`,
  positive `amount` (NUMERIC(19,4)), `currency`, `postedAt`, `memo`. Append-only:
  never updated or deleted.
- A money movement = two entries sharing one `transactionId` (a DEBIT + a CREDIT).

## Operations (port: `LedgerService`)
| Method | Purpose |
|--------|---------|
| `postDoubleEntry(...)` | Append a balanced DEBIT+CREDIT pair under one transactionId |
| `postLeg(...)` | Append a single leg (used by reversals) |
| `balanceOf(walletId)` | `SUM(CREDIT) - SUM(DEBIT)` over the wallet's entries |
| `entriesOf(transactionId)` | Fetch all legs of a transaction (for reversal) |

## Key rules
- **`MANDATORY` propagation** on writes — the ledger must run inside the caller's
  transaction (e.g. a transfer), never open its own. This guarantees the ledger
  commits atomically with the rest of the money movement.
- **Amount is always positive**; the sign comes from `Direction`. Enforced by a
  SQL `CHECK (amount > 0)` in `V2__ledger.sql`.
- **Append-only at the privilege level** — `V2` notes that no UPDATE/DELETE grants
  should exist on `ledger_entries` in production.

## Code pointers
- Balance query: `LedgerEntryRepository.deriveBalance`
- Atomic posting: `DefaultLedgerService.postDoubleEntry`

## Related requirements
FR-5.1, FR-5.8, FR-5.9, NFR-2.1, NFR-2.5, NFR-2.6
