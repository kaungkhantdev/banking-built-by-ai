# Phase 4 — Implementation / Sprint Plan

> Digital Banking Platform · SDLC Phase 4 of N
> Status: Delivery Plan · Owner: Tech Lead + Product Owner + Delivery Lead

---

## 4.1 Purpose

Turn the approved Design (Phase 3) into an executable delivery plan: a
prioritized product backlog, a milestone map, a sprint-by-sprint breakdown, the
team's working agreements (Definition of Ready / Done), and the tracking metrics
that tell us whether we're on course. The goal of M3–M7 from Planning is a
production launch in a single region in ~12–14 build weeks (6–7 two-week
sprints) with a small senior team.

---

## 4.2 Delivery Approach

- **Cadence:** 2-week sprints, Scrum-lite (planning, daily sync, review, retro).
- **Branching:** trunk-based with short-lived feature branches; PR + 1 review +
  green CI to merge. No direct pushes to `main`.
- **Vertical slices:** each story delivers an end-to-end, demoable increment
  (API + persistence + tests + metrics), never a horizontal layer alone.
- **Walking skeleton first:** stand up gateway → service → DB → CI/CD on day one
  so every later story plugs into a working pipeline.
- **Compliance/observability are acceptance criteria,** not separate stories —
  a money-moving story isn't "done" without audit + metrics + tests.

---

## 4.3 Estimation Scale

Story points use a modified Fibonacci scale: **1, 2, 3, 5, 8, 13**.
- 1–2: trivial, well understood.
- 3–5: normal story, some unknowns.
- 8: large or risky; consider splitting.
- 13: too big — **must** be split before entering a sprint.

Indicative team velocity assumption: **~24–28 pts/sprint** for a squad of
~4 engineers. Treat as a forecast, recalibrate after Sprint 2.

---

## 4.4 Epics → Milestones

| Epic | Title                                   | Maps to Milestone | Maps to Requirements        |
|------|-----------------------------------------|-------------------|-----------------------------|
| E0   | Platform skeleton & CI/CD               | M3                | NFR7, NFR11                 |
| E1   | AuthN/AuthZ (JWT + refresh + RBAC)      | M4                | FR-A2, FR-H, NFR1           |
| E2   | Accounts & Wallets                      | M3                | FR-A, FR-B                  |
| E3   | Ledger, Transactions & Transfers        | M3                | FR-C, FR-D, NFR2, NFR10     |
| E4   | KYC workflow                            | M4                | FR-E, NFR8                  |
| E5   | Audit trail                             | M4                | FR-F, NFR6                  |
| E6   | Event backbone (outbox + consumers)     | M5                | FR-I3, FR-I4, NFR10         |
| E7   | API Gateway & rate limiting             | M5                | FR-I1, FR-I2                |
| E8   | Observability (metrics/Grafana/tracing) | M5                | FR-I5, FR-I6, FR-I7, NFR7   |
| E9   | Admin dashboard                         | M4/M5             | FR-G                        |
| E11  | Customer mobile app (iOS + Android)     | M4/M5             | FR-A, FR-B, FR-D, FR-E, NFR1 |
| E10  | Hardening, security review, launch      | M6/M7             | NFR1, NFR3, NFR12           |

---

## 4.5 Product Backlog (prioritized)

Legend — Priority: P0 (must, blocks others) · P1 (core) · P2 (important) ·
P3 (nice). Pts = story points.

### E0 · Platform skeleton & CI/CD
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-001 | Repo, service scaffolding, containerization                          | P0  | 3   |
| S-002 | CI pipeline: build, lint, unit tests, security scan                  | P0  | 5   |
| S-003 | CD to staging with health gates + rollback                          | P0  | 5   |
| S-004 | Managed RDBMS provisioned with PITR; migration tooling              | P0  | 3   |
| S-005 | Walking skeleton: gateway → sample service → DB round-trip          | P0  | 3   |

### E1 · AuthN/AuthZ
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-010 | User credential store + registration endpoint                        | P0  | 3   |
| S-011 | Login issues signed JWT (short TTL)                                   | P0  | 5   |
| S-012 | Refresh token issue + **rotation** + hashed storage                  | P0  | 5   |
| S-013 | Refresh-reuse detection → session revocation (BR8)                   | P1  | 3   |
| S-014 | RBAC model: roles, permissions, user-roles (multi-role)             | P0  | 5   |
| S-015 | Authorization middleware (default-deny, per-permission)             | P0  | 5   |
| S-016 | Logout / revoke-all-sessions                                         | P2  | 2   |

### E2 · Accounts & Wallets
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-020 | Customer entity + lifecycle states                                   | P0  | 3   |
| S-021 | Create/read customer; profile update (KYC-locked fields)            | P1  | 3   |
| S-022 | Wallet entity + lifecycle (ACTIVE/FROZEN/CLOSED)                     | P0  | 3   |
| S-023 | Balance read **derived from ledger**                                 | P0  | 5   |
| S-024 | Admin suspend/close account (RBAC + audit)                          | P1  | 3   |
| S-025 | Freeze/unfreeze wallet (RBAC + audit)                               | P1  | 2   |

### E3 · Ledger, Transactions & Transfers
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-030 | Double-entry ledger schema + append-only invariant                   | P0  | 5   |
| S-031 | Post transaction atomically (DB tx, debit+credit sum=0)             | P0  | 8   |
| S-032 | Idempotency-key enforcement on money moves (BR5)                    | P0  | 5   |
| S-033 | Insufficient-funds + available-balance (holds) checks               | P0  | 5   |
| S-034 | Wallet-to-wallet transfer (paired debit/credit)                     | P0  | 8   |
| S-035 | Transaction reversal via compensating entries (RBAC + audit)        | P1  | 5   |
| S-036 | Holds: HOLD / RELEASE lifecycle                                      | P2  | 5   |

### E4 · KYC
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-040 | KYC case entity + state machine                                      | P0  | 3   |
| S-041 | Document submission endpoint                                         | P1  | 3   |
| S-042 | Vendor integration (async submit + webhook/poll)                    | P1  | 8   |
| S-043 | Sanctions/PEP screening hook                                         | P1  | 5   |
| S-044 | KYC-gated money-out + tiered limits (BR3)                           | P0  | 5   |
| S-045 | Compliance override decision (RBAC + audit)                         | P2  | 3   |

### E5 · Audit trail
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-050 | Append-only audit store (actor, action, before/after, trace_id)     | P0  | 5   |
| S-051 | Auto-audit on all privileged/state-changing actions                 | P0  | 5   |
| S-052 | Audit query API (by entity/actor/time/action)                       | P1  | 3   |
| S-053 | Signed, tamper-evident export                                        | P2  | 5   |

### E6 · Event backbone
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-060 | Broker provisioned; topic/partition design (per-aggregate order)    | P0  | 3   |
| S-061 | Transactional outbox table + relay publisher                        | P0  | 8   |
| S-062 | Idempotent consumers (dedup by event id)                            | P0  | 5   |
| S-063 | Notification consumer                                                | P2  | 3   |
| S-064 | Dead-letter handling + replay                                       | P1  | 5   |

### E7 · Gateway & rate limiting
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-070 | API gateway: TLS, routing, JWT validation at edge                   | P0  | 5   |
| S-071 | Request/trace-ID injection + propagation                            | P0  | 3   |
| S-072 | Rate limiting (per IP / identity / endpoint), 429 + Retry-After     | P1  | 5   |
| S-073 | Rate-limit metrics + tunable policy config                          | P2  | 3   |

### E8 · Observability
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-080 | `/metrics` on every service (golden signals)                        | P0  | 5   |
| S-081 | Prometheus scrape + retention                                       | P0  | 3   |
| S-082 | Grafana dashboards (platform + money + KYC funnel + auth anomalies) | P1  | 5   |
| S-083 | Distributed tracing gateway → services → DB/broker                  | P1  | 8   |
| S-084 | SLO alerts (error budget, latency, failed transfers, DLQ growth)    | P1  | 5   |

### E9 · Admin dashboard
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-090 | Dashboard shell + auth (RBAC-aware UI)                               | P1  | 5   |
| S-091 | Search customers/wallets/transactions/KYC                           | P1  | 5   |
| S-092 | Per-entity audit trail view                                         | P2  | 3   |
| S-093 | Operator actions (suspend/freeze/reverse/override) wired to APIs    | P1  | 5   |
| S-094 | Health panel linking Grafana                                        | P3  | 2   |

### E11 · Customer mobile app (iOS + Android)
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-110 | App scaffold, navigation, design system, CI build (both platforms)   | P0  | 5   |
| S-111 | Auth: login + secure refresh-token storage (Keychain/Keystore)       | P0  | 5   |
| S-112 | Registration + onboarding flow                                       | P1  | 5   |
| S-113 | KYC document + selfie capture and upload                             | P1  | 8   |
| S-114 | Wallet list + balance + transaction history                         | P1  | 5   |
| S-115 | Transfer flow with on-device idempotency key + retry                | P0  | 8   |
| S-116 | Push notifications (TransferCompleted / KycVerified)                | P2  | 5   |
| S-117 | Biometric unlock + certificate pinning + root/jailbreak signal       | P1  | 5   |
| S-118 | Offline-tolerant read cache + backoff on writes                     | P2  | 5   |
| S-119 | App store submission + release pipeline                             | P1  | 3   |

### E10 · Hardening & launch
| ID    | Story                                                                 | Pri | Pts |
|-------|-----------------------------------------------------------------------|-----|-----|
| S-100 | Threat model review + fixes                                          | P0  | 5   |
| S-101 | Penetration test + remediation                                      | P0  | 8   |
| S-102 | Load/soak test to 10x; tune autoscaling                            | P1  | 5   |
| S-103 | DR drill: PITR restore, validate RPO≤5m / RTO≤1h                    | P1  | 5   |
| S-104 | Runbooks, on-call, alert routing                                    | P1  | 3   |
| S-105 | Compliance sign-off + audit-export validation                       | P0  | 3   |
| S-106 | Production cutover + smoke tests                                     | P0  | 3   |

**Backlog total:** ~70 stories, ≈ 324 points (incl. the mobile track, E11).

---

## 4.6 Sprint Breakdown (forecast)

> Forecast only — re-baseline after Sprint 2 actuals. Each sprint ends with a
> demoable increment.

### Sprint 1 — Walking skeleton + identity foundation
**Goal:** a request can flow gateway → service → DB in CI/CD; users can register
and log in.
Stories: S-001, S-002, S-003, S-004, S-005, S-010, S-011 · **~27 pts**
Demo: deploy to staging; register + login returns a JWT.

### Sprint 2 — Accounts, wallets, RBAC
**Goal:** customers and wallets exist; access is permission-gated.
Stories: S-012, S-014, S-015, S-020, S-022, S-023 · **~26 pts**
Demo: create customer + wallet; protected endpoint enforces RBAC.

### Sprint 3 — The ledger (core money)
**Goal:** money can be posted correctly and idempotently.
Stories: S-030, S-031, S-032, S-033, S-050 · **~28 pts**
Demo: post transactions; double-entry invariant + idempotency proven by tests.

### Sprint 4 — Transfers, audit, events
**Goal:** end-to-end transfer with audit and an event emitted via outbox.
Stories: S-034, S-035, S-051, S-060, S-061 · **~29 pts**
Demo: wallet-to-wallet transfer → balances move, audit written, event published.

### Sprint 5 — KYC + gateway + consumers
**Goal:** onboarding gated by KYC; traffic enters via gateway; events consumed.
Stories: S-040, S-042, S-044, S-070, S-062 · **~29 pts**
Demo: unverified user blocked from money-out; verified user transfers; consumer reacts.

### Sprint 6 — Observability + admin + rate limiting
**Goal:** the platform is observable and operable.
Stories: S-080, S-082, S-083, S-072, S-090, S-091 · **~30 pts** *(split if needed)*
Demo: Grafana shows golden signals + a traced transfer; admin searches entities.

### Sprint 7 — Hardening + launch
**Goal:** production-ready.
Stories: S-100, S-101, S-102, S-103, S-105, S-106 · **~27 pts**
Demo: pen-test clean, load target met, DR drill passes, compliance sign-off, cutover.

### Parallel track — Mobile app (E11)
The mobile app runs as a **parallel workstream** alongside the backend, staffed
by a dedicated mobile sub-squad. It consumes the API contracts as they
stabilize, so it trails the backend by ~1 sprint per capability:

- **MT-1 (≈ backend Sprint 2–3):** S-110 scaffold/CI, S-111 secure auth — built
  against the auth API once it's live.
- **MT-2 (≈ Sprint 4):** S-112 onboarding, S-114 wallets/history.
- **MT-3 (≈ Sprint 5):** S-115 transfers (on-device idempotency), S-113 KYC
  capture — follow the transfer + KYC APIs.
- **MT-4 (≈ Sprint 6):** S-116 push, S-117 biometric/pinning, S-118 offline.
- **MT-5 (≈ Sprint 7):** S-119 app-store submission, hardening, store review.

> Deferred to fast-follow (post-launch): S-013, S-016, S-021, S-024, S-025,
> S-036, S-041, S-043, S-045, S-052, S-053, S-063, S-064, S-071, S-073, S-081,
> S-084, S-092, S-093, S-094, S-104. Pull forward by priority as velocity allows.

---

## 4.7 Critical Path & Dependencies

```
S-001/004/005 (skeleton)
        │
        ▼
S-010/011 (auth) ──► S-014/015 (RBAC) ──────────────┐
        │                                            ▼
        ▼                              S-024/025/035/045/093 (gated ops)
S-020/022/023 (accounts/wallets)
        │
        ▼
S-030 (ledger) ─► S-031 ─► S-032/033 ─► S-034 (transfer)
                                   │
                                   ├─► S-061 (outbox) ─► S-062 (consumers)
                                   └─► S-050/051 (audit)
S-070 (gateway) underpins all external traffic; S-080/083 (observability)
should land before launch hardening.
```

**Hard rule:** nothing money-moving (S-031+) merges before the ledger invariant
tests (double-entry sum=0, idempotency) are green.

---

## 4.8 Definition of Ready (DoR)

A story may enter a sprint only when:
- [ ] Clear, testable acceptance criteria.
- [ ] Dependencies identified and unblocked (or stubbed).
- [ ] API contract / schema change sketched.
- [ ] Security & audit implications noted.
- [ ] Estimable and ≤ 8 pts (split if larger).

## 4.9 Definition of Done (DoD)

A story is done only when:
- [ ] Code merged via PR with ≥1 review and green CI.
- [ ] Unit + integration tests written and passing.
- [ ] Money-path stories: ledger-invariant + idempotency tests included.
- [ ] AuthZ enforced; privileged actions emit audit records.
- [ ] `/metrics` updated; traces span the new path.
- [ ] API docs / contract updated; versioned.
- [ ] Deployed to staging and demoed.
- [ ] No new critical/high security findings.

---

## 4.10 Roles & Ceremonies

| Ceremony          | Cadence        | Output                                  |
|-------------------|----------------|-----------------------------------------|
| Sprint Planning   | Start of sprint| Committed sprint backlog                |
| Daily Sync        | Daily, 15 min  | Blockers surfaced                       |
| Backlog Refinement| Mid-sprint     | Next sprint's stories meet DoR          |
| Sprint Review     | End of sprint  | Demoed increment, stakeholder feedback  |
| Retrospective     | End of sprint  | 1–3 process improvements                |

---

## 4.11 Tracking Metrics

| Metric                 | Purpose                                  | Healthy signal               |
|------------------------|------------------------------------------|------------------------------|
| Velocity (pts/sprint)  | Forecasting                              | Stable ±15% after Sprint 2   |
| Sprint goal hit rate   | Predictability                           | ≥ 80%                        |
| Burndown               | In-sprint progress                       | Trends to zero, no cliff     |
| Escaped defects        | Quality                                  | Near zero on money paths     |
| PR cycle time          | Flow efficiency                          | < 1 day median               |
| CI pass rate           | Pipeline health                          | > 90% on first run           |
| Test coverage (core)   | Safety on ledger/auth                    | High on money + auth modules |

---

## 4.12 Delivery Risks (delta to Planning register)

| ID  | Risk                                            | Mitigation                                          |
|-----|-------------------------------------------------|-----------------------------------------------------|
| DR1 | Ledger story (S-031) underestimated             | Spike early in Sprint 2; pair on it; invariant tests|
| DR2 | KYC vendor integration latency (S-042)          | Start vendor onboarding in Sprint 1; mock first     |
| DR3 | Velocity unknown until Sprint 2                 | Forecast, don't commit dates downstream until S2    |
| DR4 | Observability deferred → blind during hardening | S-080/083 are P0/P1, scheduled before Sprint 7      |
| DR5 | Scope creep from deferred list                  | Strict DoR gate; product owner guards the backlog   |

---

## 4.13 Success Criteria (exit gate for Phase 4 planning)

- [x] Epics mapped to milestones and requirements.
- [x] Prioritized backlog with estimates.
- [x] Sprint-by-sprint forecast with goals and demos.
- [x] Critical path, dependencies, DoR/DoD, ceremonies, and metrics defined.
- [x] Delivery-specific risks captured.

➡ Execution begins at **Sprint 1**; re-baseline the forecast after Sprint 2.
