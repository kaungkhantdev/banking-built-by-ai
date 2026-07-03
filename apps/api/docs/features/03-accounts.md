# Feature 3 — Accounts

**Package:** `com.bank.feature.accounts` · **Endpoints:** `/v1/accounts/*`

## Purpose
An **Account** is a customer's banking relationship. Wallets hang off an account.
The account has an explicit lifecycle.

## Layout
```
accounts/
├── web/        AccountController (OpenAccountRequest), dto/ (AccountView)
├── domain/     AccountService + DefaultAccountService
└── persistence/ Account, AccountStatus, AccountRepository
```

## Lifecycle
`PENDING → ACTIVE → FROZEN/CLOSED`. New accounts start `PENDING`; activation is
typically gated on KYC verification (operational policy).

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/accounts` | `account:create` | Open an account (PENDING) |
| POST | `/v1/accounts/{id}/activate` | `account:manage` | PENDING → ACTIVE |

## Key rules
- Only a `PENDING` account can be activated; otherwise `409 ACCOUNT_ILLEGAL_TRANSITION`.
- `freeze()` is available in the service for fraud holds without deleting data.

## Code pointers
- Guarded transition: `DefaultAccountService.activate`

## Related requirements
FR-3.*
