# Feature 21 — Admin Console

**Package:** `com.bank.feature.admin` · **Endpoints:** `/v1/admin/*`

## Purpose
Provide back-office operators with the ability to search customers, manage account
and wallet states, and unlock locked users. All actions are audited.

## Layout
```
admin/
├── web/        AdminController, dto/ (CustomerSearchRequest, AdminActionView)
└── domain/     AdminService + DefaultAdminService
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/admin/customers` | `admin:read` | Search customers |
| POST | `/v1/admin/accounts/{id}/freeze` | `account:manage` | Freeze an account |
| POST | `/v1/admin/wallets/{id}/freeze` | `wallet:manage` | Freeze a wallet |
| POST | `/v1/admin/users/{id}/unlock` | `admin:manage` | Unlock a locked-out user |

## Key rules
- All endpoints require an `admin:*` or elevated permission; non-admin JWTs receive `403`.
- Every action delegates to the appropriate domain service (e.g., `AccountService.freeze`,
  `WalletService.freeze`) so business rules are enforced in one place.
- Every action is wrapped in `@Audited` so the actor, target, and timestamp are
  recorded in the audit trail (Feature 8).
- Customer search supports full-text filtering by name, email, or phone and returns
  paginated results.

## Why
Routing admin actions through the same domain services as user-facing operations
ensures freeze logic, state guards, and audit hooks cannot be bypassed via the
back-office path.

## Related requirements
FR-21.*, FR-7.1
