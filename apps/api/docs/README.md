# Backend API — Documentation

Implementation docs for the Digital Banking Platform backend (`com.bank`), a
Spring Boot 3 modular monolith. These docs describe **what is actually built** in
`apps/api/src` — every class, endpoint, and permission named here exists in the
code.

## How the code is organized

```
com.bank
├── BankApplication            entry point (@EnableScheduling, @EnableAsync)
├── config/                    app configuration only (security, JWT filter, Rabbit, rate limit, keys)
├── shared/                    cross-cutting: exception/ (ApiError, ApiException, handler), entity/ (BaseAuditEntity), utils/ (Money, CurrentUser)
└── feature/                   one package per capability, each layered:
    <feature>/
      ├── web/                 controllers + DTOs (HTTP edge)
      ├── domain/              service INTERFACE + Default* impl + business logic
      └── persistence/         JPA entities + Spring Data repositories
```

Dependencies point **inward** toward `domain/`. Every service is an interface with
a `Default*` implementation; callers depend on the interface.

## Feature documents

| # | Feature | Doc | Endpoints |
|---|---------|-----|-----------|
| 1 | Authentication | [features/01-auth.md](features/01-auth.md) | `/v1/auth/*` |
| 2 | RBAC | [features/02-rbac.md](features/02-rbac.md) | `/v1/admin/users/*` |
| 3 | Accounts | [features/03-accounts.md](features/03-accounts.md) | `/v1/accounts/*` |
| 4 | Wallets | [features/04-wallets.md](features/04-wallets.md) | `/v1/wallets/*` |
| 5 | Ledger | [features/05-ledger.md](features/05-ledger.md) | (internal) |
| 6 | Transfers | [features/06-transfers.md](features/06-transfers.md) | `/v1/transfers/*` |
| 7 | KYC | [features/07-kyc.md](features/07-kyc.md) | `/v1/kyc/*` |
| 8 | Audit | [features/08-audit.md](features/08-audit.md) | `/v1/audit` |
| 9 | Events (Outbox) | [features/09-events.md](features/09-events.md) | (internal) |

## Requirements

- [functional-requirements.md](functional-requirements.md) — FR-1 … FR-N, traced to code.
- [non-functional-requirements.md](non-functional-requirements.md) — security, reliability, performance, observability, maintainability.

## Build & run

```bash
# from apps/api/
./mvnw clean test          # compile + run unit tests (no infra needed; uses H2)
./mvnw spring-boot:run     # needs PostgreSQL + RabbitMQ (see application.yml env vars)
```

Profiles: `dev` (verbose SQL), `prod` (secrets from env), `test` (H2, no broker).
With no JWT keys configured the app generates an ephemeral RSA keypair so it boots
for local development.

> The cross-cutting error envelope is `{code, message, traceId}` (see
> `shared/exception/ApiError`). The `traceId` correlates a client error to server
> logs and audit records.
