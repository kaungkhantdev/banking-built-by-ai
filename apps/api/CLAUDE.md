# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./mvnw clean test                                                    # all tests (H2, no infra)
./mvnw clean test -Dtest=DefaultTransferServiceTest                  # single test class
./mvnw spring-boot:run                                               # run locally (needs Postgres + RabbitMQ)
```

Java 21 is required (`maven.compiler.release=21`).

## Package layout

```
com.bank
├── config/          # SecurityConfig, JwtAuthFilter, RateLimitFilter, KeyProvider
├── feature/
│   ├── <name>/
│   │   ├── web/     # controllers + DTOs, @PreAuthorize guards
│   │   ├── domain/  # service interface + Default* implementation
│   │   └── persistence/  # JPA entities + Spring Data repos
│   └── events/      # outbox writer, relay, consumer
└── shared/
    ├── exception/   # ApiError, ApiException, GlobalExceptionHandler
    ├── utils/       # Money, CurrentUser
    └── entity/      # BaseAuditEntity
```

Features implemented: `auth`, `rbac`, `accounts`, `wallets`, `ledger`, `transfers`, `kyc`, `audit`, `events`, `health`.

## Critical invariants

**Money** — always use `Money.of(BigDecimal)` (scale=4, HALF_EVEN). Never use raw `double` or unchecked `BigDecimal`. The DB column is `NUMERIC(19,4)`.

**Ledger** — `ledger_entries` is append-only. Balance is always derived (`SUM(CREDIT) - SUM(DEBIT)`), never stored. Amounts are always positive; `Direction` (DEBIT/CREDIT) carries the sign. `LedgerService` methods use `Propagation.MANDATORY` — they must run inside the caller's transaction.

**Outbox** — `OutboxWriter.write()` uses `Propagation.MANDATORY`. It must be called inside a business `@Transactional` method. The relay (`OutboxRelay`) polls every 1 s after commit and publishes to `bank.events` topic exchange. Never call `OutboxWriter` outside a transaction.

**Transfer atomicity** — `DefaultTransferService.transfer()` is a single `@Transactional` unit: idempotency insert → wallet fetch → same-wallet/currency check → KYC gate → balance check → double-entry ledger → outbox write. All five commit together or roll back.

**Idempotency** — enforced by the `uq_idem_key` DB unique constraint (V2 migration). The app catches `DataIntegrityViolationException` and replays the prior result; no application-level locking.

**KYC gate** — `KycGate.assertMoneyOutAllowed(accountId)` must pass before any money-out. KYC state machine: `CREATED → DOCS_SUBMITTED → UNDER_REVIEW → VERIFIED/REJECTED`. Only `VERIFIED` unblocks transfers.

**RBAC** — permissions are baked into the RS256 JWT `perms` claim at login (no per-request DB lookup). Seeded in `V6__seed_rbac.sql`. Any new `@PreAuthorize("hasAuthority('...')")` must have a matching permission row in that migration.

## Schema

Flyway owns the schema (`src/main/resources/db/migration/V*.sql`). Hibernate is `ddl-auto: validate` — it never modifies the DB. New tables/columns always need a new `V{n}__*.sql` file.

Key migrations: V1 — users/accounts/wallets, V2 — ledger + idempotency, V3 — KYC, V4 — audit, V5 — outbox, V6 — RBAC seed, V7 — currency `VARCHAR(3)` fix.

## Testing

Tests use H2 in PostgreSQL-compat mode (`application-test.yml`). Flyway is disabled in tests — schema comes from `ddl-auto: create-drop`. RabbitMQ is fully excluded. `EventPublisher` has an in-memory test double; swap it via `@MockitoBean` or a test `@Bean`. The `realkyc` Spring profile activates the real `KycVendorClient`; default is `StubKycVendorClient`.

## Profiles

- `dev` (default locally): verbose SQL, 100% trace sampling, H2 console at `/h2-console`
- `prod`: no SQL echo, 10% sampling, requires env vars `JWT_PRIVATE_KEY_PEM`, `JWT_PUBLIC_KEY_PEM`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `RABBITMQ_*`
- Blank JWT keys in dev generate an ephemeral keypair (tokens don't survive a restart)

## Error handling

All errors return `{code, message, traceId}` via `GlobalExceptionHandler`. Throw `ApiException(code, message, httpStatus)` from domain/web layers — never throw raw exceptions that leak internals.
