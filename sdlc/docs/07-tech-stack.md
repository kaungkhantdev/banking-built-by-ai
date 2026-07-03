# Tech Stack & Architecture Style

> Digital Banking Platform · Technology Decisions
> Status: Baseline · Owner: Architect + Tech Lead

---

## 7.1 Chosen Technologies

| Layer            | Technology                    | Role                                                        |
|------------------|-------------------------------|-------------------------------------------------------------|
| Backend API      | **Java + Spring Boot**        | Core-banking services, REST API, business logic, ledger.    |
| Admin Dashboard  | **Angular**                   | Operator web console (RBAC-gated).                          |
| Mobile App       | **Flutter**                   | Customer iOS + Android app (single codebase).               |
| Database         | PostgreSQL (recommended)      | System of record for the money core.                        |
| Message broker   | RabbitMQ or Kafka             | Event delivery (start with RabbitMQ; Kafka if scale needs). |
| Auth             | JWT (access) + refresh tokens | Stateless authentication.                                   |
| Observability    | Prometheus + Grafana + tracing| Metrics, dashboards, distributed tracing.                   |

These three client/runtime choices (Spring Boot, Angular, Flutter) are fixed.
The database, broker, and observability tools are recommendations consistent
with the design and can be swapped for equivalents.

---

## 7.2 The Real Question: Microservices or Not?

A common confusion is to treat **"event-driven"** and **"microservices"** as the
same decision. They are not. They are two independent choices:

- **Event-driven** = *how components talk* — by emitting/reacting to events
  (asynchronous) instead of direct synchronous calls.
- **Microservices** = *how the code is deployed* — many small, independently
  deployed services, each owning its own database.

You can mix and match all four combinations. The most important point:

> **You can be fully event-driven inside a single application.**
> Microservices are **not** required to get the benefits of event-driven design.

### The two honest options for this project

**Option A — Modular Monolith (recommended to start).**
One Spring Boot application, internally split into clean modules (auth,
accounts, wallets, ledger, transfers, kyc, audit). Modules talk to each other
through well-defined interfaces, and **events flow internally** through the
outbox + broker exactly as designed. One deployable unit, one database for the
money core.

**Option B — Microservices.**
Each capability (auth, accounts, ledger, kyc, etc.) is its own separately
deployed Spring Boot service with its own database, communicating over the
network and via events.

---

## 7.3 Recommendation: Start with a Modular Monolith

For this platform — a first build, a team new to event-driven design, and a
hard requirement for money correctness — **start with Option A (modular
monolith)** and extract services later only where a real need appears.

### Why a monolith first (honest reasoning)

| Factor                  | Why it favors the monolith                                            |
|-------------------------|----------------------------------------------------------------------|
| **Money correctness**   | A transfer must atomically write debit + credit + outbox in **one** database transaction. In one app + one DB this is a single `@Transactional` commit. Across microservices it becomes a distributed saga with compensations — far more code and far more ways to corrupt balances. |
| **Team experience**     | Event-driven is new to the team. Learning it **inside one codebase** (no network failures, no service discovery, no distributed tracing just to debug) is dramatically simpler. |
| **Operational cost**    | One app to deploy, log, and monitor — not 6+ services, their pipelines, and the failure modes between them. |
| **Speed to launch**     | Less infrastructure plumbing = more time on actual features. |
| **Reversibility**       | Clean module boundaries mean you can extract a module into its own service **later** with low effort. Going the other way (merging premature microservices back) is painful. |

### Why NOT microservices yet (the trap to avoid)

Microservices solve problems you do **not** have at launch: independent scaling
of specific capabilities, large teams stepping on each other, polyglot stacks.
Adopting them on day one means paying their full cost — distributed
transactions, network failures, eventual consistency *everywhere*, harder
debugging — to solve problems that aren't present. That is over-engineering, the
same mistake as reaching for heavy infrastructure when a simpler design works.

> **Rule of thumb:** don't split into a service until a concrete pressure forces
> it (a module needs to scale independently, a separate team owns it, or it
> needs isolation/compliance boundaries). Split for a reason, not for fashion.

---

## 7.4 How Event-Driven Works Inside the Monolith

Even as a single Spring Boot app, the system is genuinely event-driven:

```
                 ┌──────────────────────────────────────────┐
                 │        Spring Boot application            │
                 │                                           │
   Request ─────▶│  [Transfers module]                       │
                 │     1. validate + debit/credit            │
                 │     2. write outbox row  ── same TX ──┐   │
                 │     3. COMMIT                          │   │
                 └────────────────────────────────────────┼──┘
                                                          │
                          outbox relay polls/tails ───────┘
                                      │ publishes
                                      ▼
                          ┌────────────────────┐
                          │   Message Broker    │  (RabbitMQ / Kafka)
                          └─────────┬──────────┘
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
       Notification          Audit consumer         Analytics
       consumer              (append-only)          consumer
```

- Spring's `@Transactional` makes step 1–3 a single atomic commit.
- The outbox relay (a scheduled poller, or Debezium CDC if using Kafka) reads
  committed outbox rows and publishes them to the broker.
- Consumers can run as `@RabbitListener` / Kafka listeners **inside the same
  app** to start, and be moved to separate processes later without changing the
  event contracts.
- Spring Boot also supports in-process events (`ApplicationEventPublisher`) for
  the simplest cases, but the **outbox + broker** is what gives durability and
  the ability to extract services later — so use the outbox for anything that
  must not be lost (all money-related events).

---

## 7.5 Spring Boot Module Layout (suggested)

```
com.bank
 ├── config/          app configuration only (security, JWT filter, broker)
 ├── shared/          cross-cutting: exception/, entity/ (BaseAuditEntity), utils/ (Money)
 └── feature/
     ├── auth/        JWT issue/verify, refresh rotation
     ├── rbac/        roles, permissions, enforcement
     ├── accounts/    customer lifecycle
     ├── wallets/     wallet lifecycle, balance reads
     ├── ledger/      double-entry postings, transactions  ← money core
     ├── transfers/   transfer orchestration                ← money core
     ├── kyc/         vendor orchestration, decisioning
     ├── audit/       append-only audit consumer
     └── events/      outbox table, relay, event contracts
```

Each feature under `feature/` is internally layered into **`web/`** (controllers
+ DTOs), **`domain/`** (a service *interface* + its `Default*` implementation and
business logic), and **`persistence/`** (repositories + JPA entities), with
dependencies pointing inward toward `domain/`. The `ledger` + `transfers` +
`events(outbox)` modules share the money database and one transaction boundary.
`audit`, `kyc`, notifications, and analytics consume events and can later become
their own services.

---

## 7.6 Per-Stack Notes

### Backend — Java / Spring Boot
- **Spring Web** (REST), **Spring Security** (JWT + RBAC), **Spring Data JPA**
  (PostgreSQL), **Spring AMQP / Spring for Kafka** (broker), **Flyway**
  (DB migrations), **Micrometer** (Prometheus metrics), **Spring Boot Actuator**
  (`/metrics`, health), **OpenTelemetry** (tracing).
- Money amounts use `BigDecimal` (never `double`/`float`).
- Idempotency keys enforced with a unique DB constraint.

### Dashboard — Angular
- Standalone components, signals (`signal()`, `computed()`, `toSignal()`),
  `inject()` for DI, new control flow (`@if`, `@for`).
- Lazy-loaded feature routes via `loadComponent`; functional guards and
  interceptors (`CanActivateFn`, `HttpInterceptorFn`).
- Talks only to the `/v1` REST API; holds no business logic.
- RBAC-aware UI: hide/disable actions the role lacks, **but the server still
  enforces** every permission (UI hiding is convenience, not security).
- Auth via the same JWT; access token in memory, refresh handled via
  `HttpInterceptorFn`; styled with Tailwind CSS 4.

### Mobile — Flutter
- Single Dart codebase → iOS + Android (native widgets, no WebView).
- Refresh token stored in **flutter_secure_storage** (backed by iOS Keychain /
  Android Keystore); access JWT held in memory only.
- Transfer requests carry an **on-device idempotency key** so retries over a
  flaky mobile network never double-charge.
- Certificate pinning, biometric unlock via `local_auth`, push notifications
  fed by outbox events (Firebase Messaging).

---

## 7.7 Decision Summary

| Decision                        | Choice                                                  |
|---------------------------------|---------------------------------------------------------|
| Backend                         | Java + Spring Boot                                       |
| Dashboard                       | Angular (standalone, signals, Tailwind 4)               |
| Mobile                          | Flutter (Dart, flutter_secure_storage, Riverpod)        |
| Communication style             | **Event-driven** for side-effects (outbox + broker)     |
| Money path                      | **Synchronous, ACID** (single DB transaction)           |
| Deployment style                | **Modular monolith first**, extract services on demand  |
| Database (money core)           | One shared PostgreSQL, single transaction boundary      |
| Derived stores (audit/analytics)| Separate, event-fed, eventually consistent              |

> **Bottom line:** Use event-driven design from day one (via the outbox), but
> deploy as a modular monolith — not microservices — until a concrete need
> justifies splitting. This gets the decoupling and audit-log benefits without
> the distributed-systems cost, and keeps money correctness simple.
