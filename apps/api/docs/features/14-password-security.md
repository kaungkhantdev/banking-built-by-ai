# Feature 14 — Password & Security

**Package:** `com.bank.feature.auth` (extension) · **Endpoints:** `/v1/auth/*`

## Purpose
Extend the core auth feature with self-service password management, reset flows,
brute-force lockout, and login auditing.

## Layout
```
auth/
├── web/        PasswordController, dto/ (ChangePasswordRequest, ResetPasswordRequest, InitResetRequest)
├── domain/     PasswordService + DefaultPasswordService, PasswordHistoryEntry
└── persistence/ PasswordHistory, PasswordHistoryRepository, PasswordResetToken, PasswordResetTokenRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/auth/password/change` | authenticated | Change own password |
| POST | `/v1/auth/password/reset/init` | public | Request a password-reset email |
| POST | `/v1/auth/password/reset/confirm` | public | Apply reset using one-time token |

## Key rules
- Reset tokens are high-entropy random values stored only as SHA-256 hashes, with a
  short TTL (e.g., 15 min). Presenting an expired or unknown token returns `400`.
- The last N passwords (configurable) are stored as BCrypt hashes in `PasswordHistory`;
  re-use returns `422 PASSWORD_REUSED`.
- After M consecutive failed logins (configurable) the account is temporarily locked for
  T seconds; subsequent attempts return `423 ACCOUNT_LOCKED`.
- Every login attempt (success and failure) is written to the audit trail.

## Why
Storing only hashes for both passwords and reset tokens means a DB breach cannot be
used directly to take over accounts or replay resets.

## Related requirements
FR-14.*, FR-1.2, FR-7.1
