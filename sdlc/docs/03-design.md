# Phase 3 — Design

> Digital Banking Platform · SDLC Phase 3 of 3
> Status: Architecture & Detailed Design · Owner: Architect + Security + Platform

---

## 3.1 Design Goals

- **Correctness of money** above all: double-entry ledger, atomic writes,
  idempotency, no double-spend.
- **Security & compliance by construction:** JWT + refresh rotation, RBAC,
  immutable audit trail, encrypted PII.
- **Operability:** every request is traceable; every service is measured.
- **Evolvability:** clear service boundaries, event-driven decoupling,
  versioned APIs.

---

## 3.2 High-Level Architecture

```
   ┌──────────────┐   ┌──────────────┐
   │  Mobile App  │   │    Admin     │   end-user + operator clients
   │ (iOS/Android)│   │  Dashboard   │
   └──────┬───────┘   └──────┬───────┘
          │                  │
          ▼                  ▼
                         ┌──────────────────────────────┐
       (HTTPS/JSON)      │         API GATEWAY           │
        │                │  • TLS termination            │
        ▼                │  • JWT validation             │
   ┌─────────┐           │  • Rate limiting              │
   │  Edge   │──────────▶│  • Routing / request ID       │
   └─────────┘           │  • Trace context propagation  │
                         └───────────────┬───────────────┘
                                         │ (authn'd, rate-limited)
        ┌────────────────┬───────────────┼────────────────┬───────────────┐
        ▼                ▼               ▼                ▼               ▼
  ┌───────────┐   ┌───────────┐   ┌────────────┐   ┌──────────┐   ┌──────────┐
  │   Auth    │   │ Accounts  │   │  Ledger /  │   │   KYC    │   │  Audit   │
  │  Service  │   │ & Wallets │   │ Transfers  │   │ Service  │   │ Service  │
  └─────┬─────┘   └─────┬─────┘   └─────┬──────┘   └────┬─────┘   └────┬─────┘
        │               │               │               │              │
        │       writes  │       writes  │   OUTBOX      │ vendor       │ append
        ▼               ▼               ▼  events       ▼ (async)      ▼ only
  ┌──────────────────────────────────────────────────────────────────────────┐
  │                         PRIMARY DATABASE (RDBMS)                            │
  │   accounts · wallets · ledger_entries · transactions · kyc · audit · rbac  │
  └──────────────────────────────────────────────────────────────────────────┘
                                         │ (transactional outbox)
                                         ▼
                            ┌───────────────────────┐
                            │     MESSAGE BROKER     │  event-driven backbone
                            │  TransferCompleted...  │  (at-least-once)
                            └───────────┬───────────┘
                                        ▼  idempotent consumers
                  ┌──────────────┬──────────────┬──────────────┐
                  ▼              ▼              ▼              ▼
            notifications   audit sink     analytics    fraud hooks (stub)

  OBSERVABILITY (cross-cutting):
    Prometheus  ◀── /metrics from every service
    Grafana     ◀── dashboards over Prometheus + traces
    Tracing     ◀── trace_id flows gateway → services → DB/broker
```

**Style:** service-oriented + event-driven. Services own their logic and share
one strongly-consistent ledger database for money correctness; non-critical
fan-out (notifications, analytics, fraud) happens asynchronously via events.

---

## 3.3 Services / Components

| Service             | Responsibility                                                        | Key NFRs        |
|---------------------|-----------------------------------------------------------------------|-----------------|
| API Gateway         | TLS, JWT validation, routing, rate limiting, trace propagation        | NFR1, NFR4      |
| Auth Service        | Login, JWT issue, refresh-token rotation, session revocation          | NFR1            |
| Accounts & Wallets  | Customer lifecycle, wallet lifecycle, balance reads (from ledger)     | NFR2, NFR3      |
| Ledger & Transfers  | Double-entry postings, transactions, transfers, idempotency, outbox   | NFR2, NFR10     |
| KYC Service         | Document intake, vendor orchestration, decisioning, screening hooks   | NFR8            |
| Audit Service       | Append-only audit records, query, signed export                       | NFR6            |
| RBAC (lib + store)  | Roles, permissions, enforcement middleware                            | NFR1            |
| Admin Dashboard     | Operator UI over the above (RBAC-gated)                               | NFR1, NFR7      |
| Mobile App (client) | iOS/Android customer app: onboarding, KYC capture, wallets, transfers | NFR1, NFR4      |
| Event Consumers     | Notifications, analytics, fraud hooks (idempotent)                     | NFR10           |

### 3.3.1 Mobile App (customer client)

The mobile app is the primary end-user surface and a pure consumer of the
`/v1` REST API — it holds no business logic of its own.

- **Platforms:** iOS + Android via **Flutter** — a single Dart codebase
  compiling to native widgets on both platforms.
- **Auth:** stores the **refresh token in the OS secure enclave** via
  `flutter_secure_storage` (iOS Keychain / Android Keystore), never in plain
  storage; short-lived access JWT held in memory. Silent refresh + rotation per BR8.
- **Key flows:** registration, KYC document/selfie capture and upload, wallet
  balances, transfer initiation with an **idempotency key generated on-device**
  (so retries over flaky mobile networks never double-charge — BR5), and push
  notifications driven by `TransferCompleted` / `KycVerified` events.
- **Security:** certificate pinning to the gateway, biometric unlock
  (Face/Touch ID), jailbreak/root signal, no PII cached beyond what's needed.
- **Resilience:** offline-tolerant read cache; all writes are idempotent and
  retried with backoff.

---

## 3.4 Data Model (physical, relational)

```
customers
  id              UUID PK
  email           CITEXT UNIQUE
  phone           TEXT UNIQUE
  status          ENUM(PENDING,ACTIVE,SUSPENDED,CLOSED)
  kyc_status      ENUM(NOT_STARTED,PENDING,VERIFIED,REJECTED)
  created_at      TIMESTAMPTZ

wallets
  id              UUID PK
  customer_id     UUID FK → customers.id
  currency        CHAR(3)            -- ISO 4217
  status          ENUM(ACTIVE,FROZEN,CLOSED)
  created_at      TIMESTAMPTZ
  -- balance is DERIVED: SUM(ledger_entries) — never stored as source of truth

transactions
  id              UUID PK
  type            ENUM(CREDIT,DEBIT,HOLD,RELEASE,REVERSAL)
  status          ENUM(PENDING,POSTED,FAILED,REVERSED)
  idempotency_key TEXT UNIQUE        -- (key, scope) dedup
  reverses_id     UUID FK → transactions.id NULL
  metadata        JSONB
  created_at      TIMESTAMPTZ

ledger_entries                       -- immutable, append-only
  id              UUID PK
  transaction_id  UUID FK → transactions.id
  wallet_id       UUID FK → wallets.id
  direction       ENUM(DEBIT,CREDIT)
  amount          NUMERIC(20,4)      -- minor-unit safe, never float
  created_at      TIMESTAMPTZ
  CONSTRAINT: per transaction, SUM(credits) = SUM(debits)   -- double-entry

transfers
  id              UUID PK
  source_wallet   UUID FK → wallets.id
  dest_wallet     UUID FK → wallets.id
  amount          NUMERIC(20,4)
  status          ENUM(PENDING,COMPLETED,FAILED)
  transaction_id  UUID FK → transactions.id
  created_at      TIMESTAMPTZ

kyc_cases
  id              UUID PK
  customer_id     UUID FK → customers.id
  status          ENUM(PENDING,VERIFIED,REJECTED)
  vendor_ref      TEXT
  decided_by      UUID NULL          -- compliance officer (override)
  decided_at      TIMESTAMPTZ NULL

audit_records                        -- immutable, append-only
  id              UUID PK
  actor_id        UUID
  action          TEXT
  entity_type     TEXT
  entity_id       UUID
  before          JSONB
  after           JSONB
  trace_id        TEXT               -- correlate with tracing
  created_at      TIMESTAMPTZ

roles(id, name)
permissions(id, name)               -- e.g. transaction:reverse, kyc:override
role_permissions(role_id, permission_id)
user_roles(user_id, role_id)        -- multi-role: many rows per user

refresh_tokens
  id              UUID PK
  user_id         UUID
  token_hash      TEXT               -- store hash, never raw
  status          ENUM(ACTIVE,ROTATED,REVOKED)
  rotated_from    UUID NULL
  expires_at      TIMESTAMPTZ

outbox                                -- transactional outbox for events
  id              UUID PK
  aggregate_type  TEXT
  event_type      TEXT
  payload         JSONB
  published       BOOLEAN DEFAULT false
  created_at      TIMESTAMPTZ
```

**Money correctness invariants**
- Balance = `SUM(credit) − SUM(debit)` over a wallet's ledger entries.
- Available balance = balance − active holds.
- Ledger entries are never updated or deleted; reversals add new entries.
- `idempotency_key` unique constraint prevents duplicate postings.

---

## 3.5 Security Design

### Authentication — JWT + Refresh Token
- **Access token (JWT):** short TTL (e.g. 10–15 min), signed (asymmetric,
  RS256/EdDSA). Carries `sub`, `roles`, `permissions` (or a claims ref), `exp`,
  `jti`. Validated at the **gateway** and re-checked in services.
- **Refresh token:** long-lived, opaque, stored **hashed**. On use →
  **rotation**: old token marked `ROTATED`, new pair issued. **Reuse of a
  consumed token revokes the whole session** (theft detection — BR8).
- Logout / compromise → revoke all `ACTIVE` refresh tokens for the user.

### Authorization — RBAC / Multi-Role
- Roles: `customer, support, operator, compliance, admin, auditor`.
- Users hold **multiple roles**; effective permissions = union of role perms.
- Enforcement middleware on every protected route requires a named permission
  (e.g. `transaction:reverse`). Default deny.
- Sensitive actions (`kyc:override`, `transaction:reverse`, `account:suspend`)
  always produce an audit record (FR-F4).

### Data protection
- PII encrypted at rest; secrets in a vault (never in env files in repos).
- TLS in transit end to end, including service-to-service.
- Principle of least privilege for service DB accounts.

### Threat-model highlights (STRIDE-lite)
| Threat        | Control                                                        |
|---------------|----------------------------------------------------------------|
| Spoofing      | Signed JWT, gateway validation, mTLS option internally         |
| Tampering     | Immutable ledger + audit; signed audit export                  |
| Repudiation   | Full audit trail with actor + trace_id                         |
| Info disclosure | Encryption at rest/in transit, least privilege               |
| DoS           | Gateway rate limiting + broker backpressure                    |
| Elevation     | RBAC default-deny, audited privilege checks                    |

---

## 3.6 API Design (representative, versioned `/v1`)

```
POST /v1/auth/register            → create customer (PENDING)
POST /v1/auth/login               → { access_token, refresh_token }
POST /v1/auth/refresh             → rotate; new token pair
POST /v1/auth/logout              → revoke session

GET  /v1/customers/{id}                          (perm: customer:read | self)
POST /v1/customers/{id}/suspend                  (perm: account:suspend)

GET  /v1/wallets/{id}                            (perm: wallet:read | owner)
POST /v1/wallets/{id}/freeze                     (perm: wallet:freeze)

POST /v1/transfers                               (perm: transfer:create | owner)
     headers: Idempotency-Key: <uuid>
     body: { source_wallet, dest_wallet, amount, currency }
GET  /v1/transactions/{id}                       (perm: transaction:read)
POST /v1/transactions/{id}/reverse               (perm: transaction:reverse)

POST /v1/kyc/{customerId}/documents              (perm: kyc:submit | self)
POST /v1/kyc/{caseId}/decision                   (perm: kyc:override)

GET  /v1/audit?entity=&actor=&from=&to=          (perm: audit:read)
GET  /v1/audit/export                            (perm: audit:export)

GET  /metrics                                    (Prometheus scrape; internal)
```

**API conventions:** JSON; idempotency on all money-moving POSTs; pagination on
lists; standard error envelope `{ code, message, trace_id }`; every response
carries the `trace_id` for support.

---

## 3.7 Event-Driven Design

### 3.7.1 Plain-language overview

**The idea.** When something important happens, the part of the system that did
the work records a short note saying *"this happened"* and immediately moves on.
Other parts of the system that care about that fact pick up the note and react
to it on their own time. The part that did the work never waits for them.

Three terms cover the whole model:

- **Event** — a note that a fact occurred, written in the past tense
  (`TransferCompleted`, `KycVerified`). It is plain data: who, what, when,
  amounts, and a `trace_id`.
- **Producer** — the component that emits the event (here, the service that just
  changed state, e.g. Ledger & Transfers).
- **Consumer** — a component that reacts to an event (notifications, audit,
  analytics, fraud scoring). Each consumer works independently of the others.
- **Broker** — the infrastructure that holds events and delivers them to the
  interested consumers.

**The standard pattern (publish/subscribe with a log).** A producer publishes
one event; any number of consumers subscribe and each receives its own copy.
This is the same shape used across the industry — order systems emitting
`OrderPlaced`, streaming platforms emitting `VideoWatched`, logistics systems
emitting `ShipmentDispatched`. The producer does not know or care who is
listening, which is exactly the property that keeps it decoupled.

**What is and is not event-driven here (important).** The money movement itself
is **not** event-driven. A transfer is a synchronous, immediate database
transaction: validate → debit + credit → commit → return the result to the
caller. Events are used only for the **after-effects** of a committed fact —
notify, audit, analyze, score. We never move money by emitting an event and
hoping a consumer handles it.

### 3.7.2 Why this architecture (rationale)

| Reason                         | What it buys us                                                        |
|--------------------------------|------------------------------------------------------------------------|
| **Speed / isolation**          | The core action commits in tens of ms and is not slowed or failed by a downstream (push provider, analytics) being slow or down. |
| **Decoupling / extensibility** | New side-effects (receipts, loyalty, reconciliation) are added as new consumers without modifying or redeploying the producer. |
| **Failure isolation**          | A broken or slow consumer cannot break the core action; its work simply queues until it recovers. |
| **Burst absorption**           | Traffic spikes queue in the broker instead of overwhelming services; producers and consumers scale independently. |
| **Built-in history**           | The append-only event stream *is* an ordered record of what happened — directly useful for the audit/compliance requirement. |

### 3.7.3 The trade-offs (honest costs)

- **Eventual consistency:** anything fed by events (audit view, dashboards,
  analytics) lags the source of truth by milliseconds-to-seconds. This is
  acceptable for those surfaces. **Balances are never read from a lagging
  projection** — they come straight from the money database, synchronously.
- **Operational surface:** a broker to run and monitor; consumers must be
  idempotent; dead-letter handling and replay must exist.
- **Harder debugging:** there is no single stack trace across an async
  boundary, which is why a `trace_id` is propagated through every event and log.
- **When it would be overkill:** if there were a single side-effect and no audit
  requirement, a direct in-process call would be correct and a broker would be
  over-engineering. The pattern is justified here by *multiple independent
  consumers + a hard audit requirement + bursty load + a core path that must
  stay fast*.
- **Minimum-viable form:** the outbox table + a simple relay + one or two
  consumers already delivers the decoupling and the history. A full
  high-throughput broker is adopted only when consumer count and volume demand
  it — not on day one.

### 3.7.4 Mechanics

- **Transactional outbox:** domain changes and their events are written in the
  **same DB transaction** (outbox table). A relay publishes to the broker → no
  lost events, no dual-write inconsistency.
- **Events (examples):** `CustomerRegistered`, `KycVerified`, `KycRejected`,
  `TransferCompleted`, `TransferFailed`, `TransactionReversed`,
  `AccountSuspended`.
- **Delivery:** at-least-once. **Consumers are idempotent** (dedup by event id)
  → effectively-once processing (NFR10).
- **Consumers:** notifications, audit sink, analytics, fraud hooks (stub).
- **Ordering:** per-aggregate ordering via partition key (e.g. wallet id).

---

## 3.8 API Gateway & Rate Limiting

- Single ingress: TLS, JWT validation, routing, request/trace ID injection.
- **Rate limiting** strategies (configurable, layered):
  - Per IP (coarse, anti-abuse).
  - Per authenticated identity (fair use).
  - Per endpoint (protect expensive/money-moving routes).
  - Algorithm: token bucket / sliding window; limits tunable per policy.
- On limit breach → `429 Too Many Requests` with `Retry-After`; event/metric
  emitted for monitoring (avoid false-positive lockouts — R8).

---

## 3.9 Observability Design

| Pillar   | Implementation                                                          |
|----------|-------------------------------------------------------------------------|
| Metrics  | Each service exposes `/metrics`; **Prometheus** scrapes. Golden signals (latency, traffic, errors, saturation) + business KPIs (transfers/min, KYC pass rate, balance totals). |
| Dashboards | **Grafana** boards: platform health, money-movement, KYC funnel, auth/refresh anomalies, rate-limit hits. |
| Tracing  | **Distributed tracing**: trace context created at the gateway, propagated through every service to DB/broker; spans correlate with audit `trace_id` and logs. |
| Logging  | Structured JSON logs, correlated by `trace_id`/`request_id`.            |
| Alerting | SLO-based alerts (error budget burn, latency, failed transfers, dead-letter growth). |

**Correlation:** one `trace_id` ties together gateway log → service spans →
audit record → metric exemplar. MTTD target < 5 min (O5).

---

## 3.10 Deployment & Environments

- Containerized services on an orchestrator; stateless services autoscale.
- Environments: `dev → staging → prod`; identical config shape, secrets per env.
- Database: managed RDBMS with **PITR**; read replicas for reporting.
- CI/CD: build → test (unit, ledger-invariant, contract) → security scan →
  deploy with health gates and easy rollback.
- DR: RPO ≤ 5 min, RTO ≤ 1 hr for core services (NFR12).

---

## 3.11 Key Design Decisions (ADR summary)

| # | Decision                                   | Rationale                                          |
|---|--------------------------------------------|----------------------------------------------------|
| 1 | Double-entry ledger, balance derived       | Auditability + no balance-edit bugs (BR4, BR6)     |
| 2 | Idempotency keys on money-moving POSTs      | Prevent duplicate charges on retries (BR5)         |
| 3 | Transactional outbox for events             | Avoid dual-write loss/inconsistency (NFR10)        |
| 4 | Short JWT + rotating refresh tokens         | Limit blast radius of token theft (BR8)            |
| 5 | RBAC default-deny + audited privileged ops  | Least privilege + traceability (FR-H, FR-F)        |
| 6 | Shared ledger DB, async fan-out via events  | Strong money consistency, loose coupling elsewhere |
| 7 | NUMERIC money, never float                  | Avoid rounding errors in financial math            |
| 8 | Trace_id stitched into audit records        | One-click incident correlation (O5)                |

---

## 3.12 Success Criteria (exit gate for Design)

- [x] System architecture and service boundaries defined.
- [x] Physical data model with money-correctness invariants specified.
- [x] Security design (JWT+refresh, RBAC, threat model) complete.
- [x] API contracts, event design, gateway/rate-limiting designed.
- [x] Observability (Prometheus, Grafana, tracing) designed and correlated.
- [x] Key decisions recorded as ADRs.

➡ Design baseline ready for **Build (M3+)**.
