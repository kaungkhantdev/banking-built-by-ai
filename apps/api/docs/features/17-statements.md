# Feature 17 — Account Statements

**Package:** `com.bank.feature.statements` · **Endpoints:** `/v1/accounts/{id}/statements/*`

## Purpose
Generate immutable monthly PDF statements for an account, covering all wallets,
and make them available for download.

## Layout
```
statements/
├── web/        StatementController, dto/ (StatementView)
├── domain/     StatementService + DefaultStatementService, StatementGenerator
└── persistence/ StatementRecord, StatementRecordRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/accounts/{id}/statements` | `account:manage` | Request a statement for a period |
| GET | `/v1/accounts/{id}/statements` | `account:read` | List available statements |
| GET | `/v1/accounts/{id}/statements/{statementId}` | `account:read` | Download PDF |

## Key rules
- Statement generation is asynchronous (`@Async`). The `POST` returns `202 Accepted`
  with a `statementId`; the client polls or waits for a notification.
- Once generated the PDF is stored in object storage (Feature 27) and its digest
  recorded in `StatementRecord`. The file is never regenerated; re-requests return
  the stored version.
- A statement covers a full calendar month and cannot be requested for the current
  in-progress month.

## Why
Immutability (write-once, read-many) ensures the document a customer downloads
today is bit-for-bit identical to the one they download in five years, satisfying
record-keeping obligations.

## Related requirements
FR-17.*
