# Feature 28 — Search

**Package:** `com.bank.feature.search` · **Endpoints:** `/v1/search/*`

## Purpose
Provide a unified search interface for customers (by name, email, phone) and
transactions (by reference number), with pagination and sorting.

## Layout
```
search/
├── web/        SearchController, dto/ (CustomerSearchResult, TransactionSearchResult, SearchPage)
└── domain/     SearchService + DefaultSearchService
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/search/customers` | `customer:read` | Search customers by name / email / phone |
| GET | `/v1/search/transactions` | `wallet:read` | Search transactions by reference number |

## Key rules
- Customer search uses a case-insensitive `ILIKE` (or full-text index) on `name`,
  `email`, and `phone`. At least one query parameter must be provided; an empty
  search returns `400`.
- Transaction search is exact-match on `reference_number` (the transfer's
  `transactionId`). Results are scoped to the requesting user's own wallets unless
  the caller holds `admin:read`.
- All results support `page` + `size` parameters with a server-side cap (e.g., 100
  per page) and return total-count metadata.
- Results are sorted by relevance for text search and by `posted_at DESC` for
  transaction search.

## Why
Scoping transaction search to the requesting user's wallets by default prevents a
regular user from enumerating other users' transactions even if they guess a
reference number.

## Related requirements
FR-28.*
