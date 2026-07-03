# Feature 7 — KYC

**Package:** `com.bank.feature.kyc` · **Endpoints:** `/v1/kyc/*`

## Purpose
Know-Your-Customer onboarding modeled as a **case state machine**, with async
vendor verification and a gate that blocks money-out for unverified customers.

## Layout
```
kyc/
├── web/        KycController (OpenCaseRequest, StatusView)
├── domain/     KycService + DefaultKycService, KycVendorClient + StubKycVendorClient, KycGate
└── persistence/ KycCase, KycStatus, KycCaseRepository
```

## State machine (`KycCase.transitionTo`)
```
CREATED → DOCS_SUBMITTED → UNDER_REVIEW → VERIFIED   (terminal)
                                        → REJECTED → DOCS_SUBMITTED (re-submit)
```
Illegal jumps (e.g. `CREATED → VERIFIED`) throw `409 KYC_ILLEGAL_TRANSITION`.

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/kyc` | `kyc:submit` | Open a case for an account (idempotent) |
| POST | `/v1/kyc/{accountId}/documents` | `kyc:submit` | Upload a document (multipart); triggers async verify |
| GET  | `/v1/kyc/{accountId}/status` | `kyc:read` | Current case status |

## Key rules
- **Async verification:** `submitDocument` transitions to `DOCS_SUBMITTED` then
  hands off to `@Async orchestrateAsync`, which moves to `UNDER_REVIEW`, calls the
  vendor off the request thread, and applies the result (`VERIFIED`/`REJECTED`).
- **Money-out gate:** `KycGate.assertMoneyOutAllowed(accountId)` throws
  `403 KYC_REQUIRED` unless the case is `VERIFIED`. Called by the transfer path.
- **Two implementations** of `KycVendorClient` — `StubKycVendorClient` (default,
  approves deterministically so the platform runs end-to-end) and a real HTTP one
  activated by the `realkyc` profile. This is one of the two genuine interface
  seams in the codebase (the other is `EventPublisher`).

## Why
KYC is inherently slow (third-party vendor). We must not block a money transaction
on a vendor call, and must not let unverified customers move funds. A state machine
makes legal transitions explicit.

## Code pointers
- State machine: `KycCase.transitionTo`
- Async orchestration: `DefaultKycService.orchestrateAsync`
- Gate: `KycGate` (consumed by `DefaultTransferService`)
- Test: `KycCaseTest`

## Related requirements
FR-6.*, NFR-3.5
