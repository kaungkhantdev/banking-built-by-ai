# Feature 1 — Authentication

**Package:** `com.bank.feature.auth` · **Endpoints:** `/v1/auth/*` (public)

## Purpose
Register users, issue stateless RS256 access tokens, and manage rotating refresh
tokens with theft detection.

## Layout
```
auth/
├── web/        AuthController, dto/ (RegisterRequest, LoginRequest, RefreshRequest, UserView)
├── domain/     AuthService + DefaultAuthService, JwtService + DefaultJwtService, TokenPair, Tokens
└── persistence/ User, UserRepository, RefreshToken, RefreshTokenRepository
```

## Endpoints
| Method | Path | Auth | Body / Result |
|--------|------|------|---------------|
| POST | `/v1/auth/register` | public | `{email,password}` → `UserView` (201) |
| POST | `/v1/auth/login` | public | `{email,password}` → `TokenPair` |
| POST | `/v1/auth/refresh` | public | `{refreshToken}` → `TokenPair` |

## Key rules
- **Access token:** RS256 JWT, default 10-min TTL, carries `sub` (user id), `email`,
  and a `perms` array. Signed with the private key from `KeyProvider`.
- **Refresh token:** opaque high-entropy secret. Only its SHA-256 hash is stored
  (`RefreshToken.tokenHash`). Single-use — each refresh marks the old one `ROTATED`.
- **Theft detection:** presenting a `ROTATED`/`REVOKED`/expired token calls
  `RefreshTokenRepository.revokeSession(sessionId)` — the whole family dies, fail-safe.
- Passwords hashed with BCrypt strength 12 (`SecurityConfig.passwordEncoder`).

## Why
Stateless JWT means no DB hit to validate a request. Asymmetric RS256 means only
the auth module holds the signing key; everyone else verifies with the public key.
Rotation gives long sessions without long-lived bearer credentials.

## Code pointers
- Rotation + reuse logic: `DefaultAuthService.refresh`
- Token mint/verify: `DefaultJwtService`
- Filter that authenticates each request: `config/JwtAuthFilter`
- Test: `DefaultAuthServiceReuseTest` (proves family revoke on reuse)

## Related requirements
FR-1.*, NFR-1.1–1.5
