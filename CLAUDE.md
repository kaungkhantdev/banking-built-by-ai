# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo layout

```
bank-poc/
├── apps/api/      # Spring Boot 3 modular monolith — the source of truth for all money and auth
├── apps/web/      # Angular 22 operator console (Tailwind 4, pnpm)
├── apps/mobile/   # Flutter customer app (Riverpod, go_router)
├── sdlc/          # SDLC paper trail (planning → deployment)
└── docker-compose.yml
```

Each app has its own `docs/features/` directory with per-feature specs (FRs, endpoints, key rules, code pointers).

## Commands

### API (`apps/api`) — Spring Boot 3, Java 17+, Maven

```bash
./mvnw clean test            # run all tests (uses H2 in-memory)
./mvnw clean test -pl . -Dtest=DefaultTransferServiceTest  # single test class
./mvnw spring-boot:run       # run locally (needs Postgres + RabbitMQ — see Docker below)
```

### Web (`apps/web`) — Angular 22, pnpm

```bash
pnpm install
pnpm start        # dev server at http://localhost:4200, proxies /v1 → :8080
pnpm build        # production bundle to dist/
pnpm test         # Vitest unit tests
```

### Mobile (`apps/mobile`) — Flutter 3.22+

```bash
flutter pub get
flutter run       # runs on connected device/emulator
flutter test
```

### Docker (full local stack)

```bash
docker compose up -d --build         # postgres → rabbitmq → api → web
docker compose down -v               # stop and wipe DB
docker compose logs -f api
```

Services: web `localhost:8080`, api `localhost:8081`, RabbitMQ UI `localhost:15672` (guest/guest), Postgres `localhost:5433` (bank/bank).

## API architecture

**Modular monolith** — one deployable JAR, features separated by package (`com.bank.feature.<name>`). Each feature owns three sub-layers:

- `web/` — Spring MVC controllers + DTOs, `@PreAuthorize` permission guards
- `domain/` — service interface + `Default*` implementation, business logic
- `persistence/` — JPA entities + Spring Data repositories, Flyway migrations own the schema

**Cross-cutting shared code** lives in `com.bank.shared`:
- `ApiError` / `ApiException` / `GlobalExceptionHandler` — uniform `{code, message, traceId}` error envelope
- `Money` — `BigDecimal` with scale=4, HALF_EVEN rounding (always use this, never raw `double`)
- `CurrentUser` — extracts the authenticated user from `SecurityContextHolder`
- `BaseAuditEntity` — `createdAt` / `updatedAt` JPA superclass

**The money path** (most critical): `POST /v1/transfers` → `DefaultTransferService.transfer` executes seven steps in a single `@Transactional` unit: idempotency insert → wallet/KYC validation → ledger double-entry → outbox event. All seven commit atomically or roll back together.

**Transactional outbox** (`com.bank.feature.events`): `OutboxWriter` (with `MANDATORY` propagation) writes an `OutboxEvent` row inside the business transaction. `OutboxRelay` polls every 1s and publishes to the `bank.events` topic exchange. `TransferNotificationConsumer` deduplicates on the event id via `processed_events`.

**RBAC**: permissions are embedded in the RS256 JWT `perms` claim at login — no per-request DB lookup. `JwtAuthFilter` converts them to `GrantedAuthority`. Roles and permission strings are seeded in `V6__seed_rbac.sql` and must match `@PreAuthorize("hasAuthority('...')")` in controllers exactly.

**Ledger invariants** (`com.bank.feature.ledger`): balance is always derived (`SUM(CREDIT) - SUM(DEBIT)`) — never stored. Amounts are always positive; sign comes from `Direction` (DEBIT/CREDIT). Ledger writes use `MANDATORY` propagation — they must run inside the caller's transaction.

**KYC** is a state machine: `CREATED → DOCS_SUBMITTED → UNDER_REVIEW → VERIFIED/REJECTED`. `KycGate.assertMoneyOutAllowed` blocks transfers unless the account's KYC case is `VERIFIED`. Verification is async (`@Async`) via `StubKycVendorClient` by default; activate the `realkyc` profile for a real vendor.

**Two genuine interface seams** in the codebase: `EventPublisher` (production: `OutboxWriter`, test: in-memory) and `KycVendorClient` (production: real HTTP, default: `StubKycVendorClient`).

## Database

Schema is owned by Flyway (`src/main/resources/db/migration/V*.sql`). Hibernate is set to `ddl-auto: validate` — it never modifies the schema. New columns or tables always require a new migration file.

Key migrations: `V1` core tables, `V2` ledger + idempotency (`uq_idem_key` unique constraint), `V3` KYC, `V4` audit, `V5` outbox, `V6` RBAC seed, `V7` currency `VARCHAR(3)` fix (aligns JPA mapping with Postgres).

## Web (Angular) architecture

Standalone components throughout (no `NgModule`). Pattern: `inject()` for DI, signals for reactive state, lazy-loaded feature routes. All HTTP goes through `AuthInterceptor` (attaches Bearer token) and `ErrorInterceptor`. Dev proxy (`proxy.conf.json`) forwards `/v1/*` to `localhost:8080`.

## Configuration / profiles

API profiles: `dev` (default locally) uses H2 console, verbose SQL, 100% trace sampling. `prod` turns off SQL echo, 10% sampling, expects real PEM keys via env vars (`JWT_PRIVATE_KEY_PEM`, `JWT_PUBLIC_KEY_PEM`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `RABBITMQ_*`). Blank JWT keys generate an ephemeral dev keypair (tokens won't survive a restart).

Public endpoints (no token required): `/v1/auth/register`, `/v1/auth/login`, `/v1/auth/refresh`, `/actuator/health/**`, `/actuator/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`.
