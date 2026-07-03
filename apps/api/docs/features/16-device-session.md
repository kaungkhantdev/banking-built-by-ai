# Feature 16 — Device & Session Management

**Package:** `com.bank.feature.sessions` · **Endpoints:** `/v1/sessions/*`

## Purpose
Give users visibility into and control over their active sessions and the devices
that have logged in to their account.

## Layout
```
sessions/
├── web/        SessionController, dto/ (SessionView, DeviceView)
├── domain/     SessionService + DefaultSessionService
└── persistence/ DeviceRecord, DeviceRecordRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/sessions` | authenticated | List active sessions |
| DELETE | `/v1/sessions/{sessionId}` | authenticated | Revoke a specific session |
| DELETE | `/v1/sessions` | authenticated | Revoke all sessions except current |

## Key rules
- A **session** corresponds to a refresh-token family (shared `sessionId` on
  `RefreshToken`). Revoking a session calls `RefreshTokenRepository.revokeSession`.
- Each login records a `DeviceRecord` containing user-agent, IP, and a derived
  device fingerprint. New-device logins trigger a notification (Feature 13).
- Device fingerprint is stored for security monitoring; it is not used as an
  authentication factor.
- The current session cannot be included in a bulk-revoke to prevent accidental
  self-lockout; it must be revoked explicitly.

## Why
Letting users see and revoke sessions limits the blast radius of a compromised
refresh token: the user can terminate the attacker's session without resetting
their password.

## Related requirements
FR-16.*, FR-1.6
