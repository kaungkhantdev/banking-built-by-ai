# Feature 15 — Multi-Factor Authentication

**Package:** `com.bank.feature.mfa` · **Endpoints:** `/v1/mfa/*`

## Purpose
Add a second authentication factor via TOTP (Time-based One-Time Password) compatible
with standard authenticator apps (Google Authenticator, Authy). Sensitive operations
can require MFA re-authentication even within an active session.

## Layout
```
mfa/
├── web/        MfaController, dto/ (MfaEnrollResponse, MfaVerifyRequest, RecoveryCodesView)
├── domain/     MfaService + DefaultMfaService, TotpValidator
└── persistence/ MfaCredential, MfaCredentialRepository, RecoveryCode, RecoveryCodeRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/mfa/enroll` | authenticated | Begin TOTP enrolment; returns QR / secret |
| POST | `/v1/mfa/enroll/confirm` | authenticated | Confirm enrolment with first TOTP code |
| POST | `/v1/mfa/verify` | authenticated | Verify a TOTP code (step-up auth) |
| GET | `/v1/mfa/recovery-codes` | authenticated | View remaining recovery codes |
| POST | `/v1/mfa/recovery-codes/regenerate` | authenticated | Re-generate recovery codes |
| DELETE | `/v1/mfa/disable` | authenticated + MFA | Disable MFA |

## Key rules
- The TOTP shared secret is stored encrypted at rest; it is never returned after
  initial enrolment confirmation.
- Ten single-use recovery codes are generated at enrolment; each is stored as a
  BCrypt hash and consumed on use.
- A JWT `mfa_verified` claim (short-lived, separate from the access token) is issued
  after a successful TOTP verify to gate sensitive operations.
- Enrolment is only confirmed after the user supplies a valid TOTP code, preventing
  lockout from a mis-scanned QR code.

## Why
TOTP is offline-capable (no SMS dependency) and immune to SIM-swap attacks. Recovery
codes ensure the user cannot be permanently locked out if they lose their device.

## Related requirements
FR-15.*
