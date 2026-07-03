# Phase 5 — Testing Strategy & Quality Assurance

> Digital Banking Platform · SDLC Phase 5 of N
> Status: Test Plan · Owner: QA Lead + Tech Lead + Security

---

## 5.1 Purpose

Define how we prove the platform is correct, secure, and operable before it
touches real money. In a regulated fintech the cost of a defect is asymmetric —
a single ledger bug can mint or destroy funds — so testing is treated as a
first-class deliverable with hard gates, not an afterthought. This document
sets the test pyramid, environments, coverage targets, concrete test cases for
every critical flow, non-functional test plans, and the exit criteria that
block a release.

---

## 5.2 Quality Objectives

| ID  | Objective                                              | Target / Gate                          |
|-----|--------------------------------------------------------|----------------------------------------|
| Q1  | Money correctness                                      | 0 ledger-invariant violations, ever    |
| Q2  | No duplicate money movement                            | Idempotency proven under concurrency   |
| Q3  | Access control enforced                                | 0 unauthorized-access escapes          |
| Q4  | Audit completeness                                     | 100% of state changes audited          |
| Q5  | Performance within SLO                                 | Meets NFR4 latency under target load   |
| Q6  | Security posture                                       | 0 critical/high vulns at launch        |
| Q7  | Resilience                                             | Survives broker/DB/vendor outage tests |
| Q8  | Regression safety                                      | Core paths covered, CI green to merge  |

---

## 5.3 Test Pyramid & Mix

```
                ▲  fewer, slower, broader
        ┌───────────────┐
        │   E2E / UAT   │   ~5%   full user journeys, staging
        ├───────────────┤
        │  Integration  │  ~25%   service + DB + broker + gateway
        ├───────────────┤
        │     Unit      │  ~70%   domain logic, pure + fast
        └───────────────┘
                ▼  many, fast, narrow

   Cross-cutting (run continuously): contract, security, performance, chaos
```

- **Unit (~70%):** domain rules in isolation — balance math, state machines,
  RBAC permission resolution, token rotation logic. Milliseconds, no I/O.
- **Integration (~25%):** real DB + broker + gateway wired together; the
  money-movement and event paths live here.
- **E2E (~5%):** a handful of full journeys against a deployed staging stack.
- **Continuous:** contract tests (API/event schemas), security scans,
  performance and chaos suites on a schedule + pre-release.

---

## 5.4 Test Types & Ownership

| Type            | What it proves                                   | Owner        | When            |
|-----------------|--------------------------------------------------|--------------|-----------------|
| Unit            | Domain logic correctness                         | Engineers    | Every commit/CI |
| Integration     | Services interact correctly with infra           | Engineers/QA | Every CI run    |
| Contract        | API + event schemas stay backward-compatible     | Engineers    | Every CI run    |
| Property-based  | Invariants hold over random inputs               | Engineers    | Every CI run    |
| E2E             | User journeys work end to end                    | QA           | Per merge to main / nightly |
| Performance     | Latency + throughput meet SLO                    | QA/SRE       | Pre-release + weekly |
| Security        | No exploitable vulns; authz holds                | Security/QA  | CI + pre-release |
| Chaos/Resilience| Graceful degradation under failure               | SRE          | Pre-release     |
| UAT             | Stakeholders accept the behavior                 | Product      | Before launch   |
| DR              | RPO/RTO met on restore                           | SRE          | Pre-release     |

---

## 5.5 Test Environments & Data

| Env       | Purpose                          | Data                                  |
|-----------|----------------------------------|---------------------------------------|
| Local     | Dev inner loop, unit + container | Ephemeral, seeded fixtures            |
| CI        | Automated unit/integration/contract | Ephemeral containers, factory data |
| Staging   | E2E, performance, UAT, security  | Synthetic, prod-like volume, **no real PII** |
| Prod      | Live; smoke tests only           | Real (read-only verification)         |

**Data rules**
- No production PII in lower environments — use synthetic/anonymized data.
- KYC vendor and external rails are **mocked** in CI/local; a vendor **sandbox**
  is used in staging.
- Every test that moves money runs in an isolated DB transaction or a fresh
  schema; tests must be independent and order-agnostic.

---

## 5.6 Critical Test Cases

> Format: ID · Pre-condition → Action → Expected. P0 cases are release blockers.

### TC-A · Ledger & Money Correctness (P0)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-A1  | Post transaction → SUM(credits) == SUM(debits) for that transaction (=0).|
| TC-A2  | Debit > available balance → rejected; **no** ledger rows written.        |
| TC-A3  | Wallet balance == SUM(credit) − SUM(debit) over its entries, always.     |
| TC-A4  | Ledger entry is immutable: UPDATE/DELETE attempt is rejected/blocked.    |
| TC-A5  | Available balance excludes active holds (balance − holds).               |
| TC-A6  | Reversal posts compensating entries; net effect == 0; original intact.   |
| TC-A7  | Money uses fixed-precision decimal; 0.1+0.2 style rounding never drifts. |
| TC-A8  | Negative or zero amount transfer → rejected by validation.               |

### TC-B · Idempotency & Concurrency (P0)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-B1  | Same Idempotency-Key twice → exactly one posting; 2nd returns 1st result.|
| TC-B2  | 100 concurrent transfers from one wallet → no overspend, no lost update. |
| TC-B3  | Two transfers racing on the same balance → serialized, balance correct.  |
| TC-B4  | Retry after timeout (client unsure) → no double charge.                  |
| TC-B5  | Idempotency key expires after window → new key required for replay.      |

### TC-C · AuthN / Tokens (P0/P1)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-C1  | Login with valid creds → access JWT + refresh token issued.              |
| TC-C2  | Expired access token → 401; refresh succeeds → new pair.                 |
| TC-C3  | Refresh rotation: old refresh token marked ROTATED, unusable.            |
| TC-C4  | **Reuse of a consumed refresh token → entire session revoked** (BR8).    |
| TC-C5  | Tampered/invalid JWT signature → 401, no access.                         |
| TC-C6  | Logout → all active refresh tokens for the user revoked.                 |

### TC-D · Authorization / RBAC (P0)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-D1  | Customer calls admin-only endpoint → 403, action audited.               |
| TC-D2  | Operator without `transaction:reverse` → 403 on reversal.                |
| TC-D3  | Multi-role user gets union of permissions; each enforced.                |
| TC-D4  | Default-deny: endpoint with no granted permission → 403.                 |
| TC-D5  | Auditor (read-only) cannot mutate any resource.                          |
| TC-D6  | Privilege-escalation attempt (forged role claim) → denied + audited.     |

### TC-E · KYC (P0/P1)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-E1  | New customer is NOT_STARTED; money-out blocked (BR3).                    |
| TC-E2  | Submit docs → PENDING; vendor VERIFIED → account ACTIVE, wallet created. |
| TC-E3  | Vendor REJECTED → routed to compliance queue; money-out stays blocked.   |
| TC-E4  | Sub-limit tier: unverified can transact up to configured cap only.       |
| TC-E5  | Sanctions/PEP hit → flagged, blocked, compliance review.                 |
| TC-E6  | Compliance override → state changes, fully audited with actor + reason.  |

### TC-F · Audit Trail (P0)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-F1  | Every state change writes actor, action, before/after, timestamp.        |
| TC-F2  | Audit record is append-only; edit/delete attempt rejected.               |
| TC-F3  | Audit record carries the request trace_id (correlation works).           |
| TC-F4  | Privileged actions (suspend/freeze/reverse/override) always audited.     |
| TC-F5  | Audit export is signed/tamper-evident; tampering detectable.             |

### TC-G · Transfers End-to-End (P0)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-G1  | Verified A → verified B transfer: balances move, event emitted, audited. |
| TC-G2  | Source wallet FROZEN → transfer rejected, no balance change.             |
| TC-G3  | Source KYC not VERIFIED → rejected (BR3).                                |
| TC-G4  | Insufficient funds → 422, TransferFailed event + audit, no movement.     |
| TC-G5  | Transfer to CLOSED/nonexistent destination → rejected.                   |

### TC-H · Events / Outbox (P0/P1)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-H1  | Outbox row written in the **same DB tx** as the ledger change (atomic).  |
| TC-H2  | DB commits but relay crashes → event still published on recovery.        |
| TC-H3  | Duplicate delivery → idempotent consumer processes effectively once.     |
| TC-H4  | Poison message → dead-lettered after retries; replayable.                |
| TC-H5  | Per-aggregate ordering preserved (same wallet's events in order).        |

### TC-I · Gateway & Rate Limiting (P1)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-I1  | Unauthenticated request to protected route → 401 at the gateway.         |
| TC-I2  | Over-limit client → 429 with Retry-After; metric incremented.            |
| TC-I3  | Limit is per-identity/endpoint; one abuser doesn't block others.         |
| TC-I4  | trace_id injected at edge and propagated to downstream services.         |

### TC-M · Mobile App (P0/P1)
| ID     | Scenario                                                                 |
|--------|--------------------------------------------------------------------------|
| TC-M1  | Refresh token stored in Keychain/Keystore, never in plain storage.       |
| TC-M2  | Transfer retried over flaky network reuses on-device idempotency key → no double charge. |
| TC-M3  | Access token expiry → silent refresh + rotation; user not logged out.    |
| TC-M4  | Biometric unlock gates app open; failure blocks access to balances.      |
| TC-M5  | Certificate pinning: MITM/proxy with bad cert → connection refused.      |
| TC-M6  | KYC capture uploads documents; PENDING state reflected in UI.            |
| TC-M7  | Offline → cached balances shown read-only; writes queued + retried.      |
| TC-M8  | Push on TransferCompleted / KycVerified delivered and deep-links.        |
| TC-M9  | Logout clears tokens + cached PII from the device.                       |

---

## 5.7 Non-Functional Test Plans

### Performance / Load (NFR4, NFR5)
- **Load test:** sustained target RPS; assert p95 core reads < 300 ms, transfers
  < 800 ms p95.
- **Stress test:** ramp to 10x; find the knee, confirm graceful degradation
  (429s, queueing) rather than data corruption or crashes.
- **Soak test:** target load for 8–24h; watch for memory leaks, connection-pool
  exhaustion, DLQ growth.
- **Spike test:** sudden burst (e.g. campaign) → broker absorbs, no lost events.

### Security (NFR1, NFR9, Q6)
- **SAST** on every CI run; **dependency/SCA** scan for known CVEs.
- **DAST** against staging; **penetration test** before launch (S-101).
- AuthN/AuthZ abuse cases: token theft/replay, IDOR (access another user's
  wallet), privilege escalation, JWT tampering, injection.
- Secrets scanning in CI (no keys in repo); verify PII encrypted at rest.

### Resilience / Chaos (NFR3, NFR10, Q7)
- Kill a service instance mid-transfer → no partial postings, retry succeeds.
- Broker down → outbox buffers; events flush on recovery (TC-H2).
- KYC vendor timeout/outage → async retry, manual-review fallback, no hang.
- DB failover → in-flight tx rolls back cleanly; balances consistent after.

### Disaster Recovery (NFR12)
- PITR restore drill → validate **RPO ≤ 5 min, RTO ≤ 1 hr**; reconcile ledger
  post-restore (sum invariants still hold).

### Observability validation (NFR7)
- Assert `/metrics` exposes golden signals; a traced transfer shows a complete
  span chain gateway → service → DB/broker; alerts fire on injected failures.

---

## 5.8 Test Data & Fixtures

- **Factories/builders** for customers, wallets, transactions — no shared
  mutable global state.
- **Golden ledger fixtures:** known opening balances + a sequence of operations
  with a precomputed expected end state (regression anchor).
- **Mocks/stubs:** KYC vendor, sanctions API, external rails — with both
  happy-path and failure responses.
- **Seed personas:** unverified customer, verified customer, frozen wallet,
  compliance officer, operator, auditor, admin — one per RBAC role.

---

## 5.9 Automation & CI Gates

```
PR opened ─► lint ─► unit ─► integration ─► contract ─► SAST/SCA ─► coverage gate
                                                                        │
                                              all green + 1 review ─────┘─► merge
merge to main ─► deploy staging ─► E2E + smoke ─► nightly: perf + security + chaos
release candidate ─► pen test + load + DR drill + UAT + compliance sign-off ─► prod
```

**Hard merge gates (CI blocks the PR if any fail):**
- All unit + integration + contract tests pass.
- Coverage thresholds met on **money + auth modules** (high bar there even if
  global bar is lower).
- No new critical/high SAST or dependency findings.
- Money-path changes include ledger-invariant + idempotency tests (enforced by
  review checklist + targeted required tests).

---

## 5.10 Coverage Targets

| Area                         | Target            | Rationale                       |
|------------------------------|-------------------|---------------------------------|
| Ledger / transactions / transfers | ≥ 95% lines + branches | Money correctness is non-negotiable |
| Auth / RBAC                  | ≥ 95%             | Security-critical               |
| KYC / audit                  | ≥ 90%             | Compliance-critical             |
| Gateway / rate limiting      | ≥ 85%             | Important, lower blast radius    |
| Admin dashboard / UI         | ≥ 70%             | Lower risk, E2E covers journeys  |
| Global                       | ≥ 80%             | Baseline                        |

> Coverage is a floor, not a goal — a green % with weak assertions is worse than
> useless. Mutation testing on the ledger module is recommended to verify the
> tests actually catch faults.

---

## 5.11 Defect Management

| Severity | Definition                                  | Action                         |
|----------|---------------------------------------------|--------------------------------|
| S1 Critical | Money loss/creation, breach, data loss   | Stop-the-line; hotfix; no launch |
| S2 High  | Core flow broken, no safe workaround        | Fix before release             |
| S3 Medium| Degraded but workaroundable                 | Schedule into a sprint         |
| S4 Low   | Cosmetic / minor                            | Backlog                        |

Every S1/S2 gets a regression test added before closure (no silent fixes).

---

## 5.12 Traceability (requirement/use-case → tests)

| Requirement / Rule                | Covered by              |
|-----------------------------------|-------------------------|
| BR1/BR2 balance + sufficiency     | TC-A2, TC-A3, TC-G4     |
| BR4 immutable ledger              | TC-A4, TC-F2            |
| BR5 idempotency                   | TC-B1, TC-B4, TC-B5     |
| BR6 double-entry sum=0            | TC-A1, TC-A6            |
| BR8 refresh rotation/theft        | TC-C3, TC-C4            |
| FR-H RBAC                         | TC-D1…TC-D6             |
| FR-E KYC + BR3 gating             | TC-E1…TC-E6, TC-G3      |
| FR-F audit                        | TC-F1…TC-F5             |
| FR-D transfers (UC-2)             | TC-G1…TC-G5             |
| FR-I3/4 events/outbox             | TC-H1…TC-H5             |
| FR-I1/2 gateway + rate limit      | TC-I1…TC-I4             |
| Mobile app (E11)                  | TC-M1…TC-M9             |
| NFR4/5 performance                | §5.7 Performance        |
| NFR1/9 security                   | §5.7 Security           |
| NFR3/10 resilience                | §5.7 Resilience, TC-H2  |
| NFR12 DR                          | §5.7 DR                 |

---

## 5.13 Release Exit Criteria (Go/No-Go for launch)

- [ ] All P0 test cases pass; no open S1/S2 defects.
- [ ] Coverage targets met on money + auth + KYC + audit modules.
- [ ] Performance SLOs met at target load; stress knee understood.
- [ ] Penetration test complete; no open critical/high findings.
- [ ] Chaos suite passed (service/broker/vendor/DB failure scenarios).
- [ ] DR drill passed (RPO ≤ 5 min, RTO ≤ 1 hr); ledger reconciles post-restore.
- [ ] Audit completeness verified; signed export validated.
- [ ] Observability validated (metrics, traces, alerts firing correctly).
- [ ] UAT signed off by Product; compliance sign-off recorded.

➡ When all boxes are checked, proceed to **production cutover (S-106)**.
