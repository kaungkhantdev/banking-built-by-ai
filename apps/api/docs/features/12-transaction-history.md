# Feature 12 — Transaction History

**Package:** `com.bank.feature.history` · **Endpoints:** `/v1/wallets/{id}/transactions`

## Purpose
Expose a paginated, filterable ledger view per wallet so users can inspect every
credit and debit, with a running balance at each point in time.

## Layout
```
history/
├── web/        TransactionHistoryController, dto/ (TransactionView, HistoryPage)
└── domain/     HistoryService + DefaultHistoryService
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/wallets/{id}/transactions` | `wallet:read` | Paginated ledger for a wallet |
| GET | `/v1/wallets/{id}/transactions/export` | `wallet:read` | Stream as CSV download |

## Key rules
- Results are ordered by `posted_at DESC` with cursor-based pagination to stay stable
  under concurrent inserts.
- Filters: `from`, `to` (ISO-8601 dates), `type` (DEBIT/CREDIT), `currency`.
- Running balance is calculated as a window sum over `LedgerEntry.amount`; it is never
  stored as a column (consistent with FR-4.4).
- CSV export streams the response to avoid buffering large result sets in memory.

## Why
Deriving the running balance from the immutable ledger ensures the displayed balance
always matches the authoritative ledger without a separate projection table.

## Related requirements
FR-12.*
