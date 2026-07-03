# Phase 1 — Planning

> Digital Banking Platform · SDLC Phase 1 of 3
> Status: Baseline · Owner: Product + Engineering Leadership

---

## 1.1 Purpose & Vision

Build a **secure, compliant, event-driven digital banking platform** that lets
customers open accounts, hold money in wallets, move funds via transactions and
transfers, and pass identity verification (KYC) — while giving operators a
full audit trail, an admin dashboard, and fine-grained role-based access.

**Vision statement**
> Provide a regulator-ready core-banking backbone that any fintech or neobank
> can deploy, observe, and scale — with security, traceability, and
> operational visibility built in from day one, not bolted on later.

**Why now**
- Demand for embedded finance and neobanking continues to grow.
- Regulators (AML/KYC, PSD2-style open banking, data residency) require
  auditable, traceable systems — retrofitting compliance is expensive.
- Event-driven + observability-first architecture reduces incident cost and
  enables horizontal scale.

---

## 1.2 Business Objectives

| ID  | Objective                                          | Success Metric                                  |
|-----|----------------------------------------------------|-------------------------------------------------|
| O1  | Enable end-to-end customer onboarding              | < 5 min from signup to verified (KYC) account   |
| O2  | Process money movement reliably                    | 99.99% transaction durability, zero double-spend|
| O3  | Meet audit & compliance requirements               | 100% of state changes captured in audit trail   |
| O4  | Operate securely                                   | 0 critical vulns in prod; all access via RBAC   |
| O5  | Provide operational visibility                     | MTTD < 5 min via metrics/traces/alerts          |
| O6  | Scale horizontally                                 | 10x load with linear cost, no schema rewrite    |

---

## 1.3 Scope

### In scope (this initiative)
- Customer account lifecycle (create, suspend, close).
- Wallet management (one or more wallets per customer, multi-currency-ready).
- Transactions (credits, debits, holds, reversals) with ledger integrity.
- Transfers (intra-platform wallet-to-wallet; rails-ready abstraction for
  external).
- KYC workflow (document submission, verification states, decisioning hooks).
- Audit logs / immutable audit trail.
- Admin dashboard (operator console).
- **Customer mobile app (iOS + Android)** — primary end-user client for
  onboarding, wallets, transfers, and KYC.
- RBAC with a multi-role system.
- Security: JWT access tokens + refresh tokens.
- Platform: API Gateway, rate limiting, event-driven backbone.
- Observability: Prometheus metrics, Grafana dashboards, distributed tracing.

### Out of scope (explicitly, for v1)
- Card issuing / physical cards.
- Direct integration with national payment rails (abstracted, not built).
- Lending, credit scoring, interest accrual.
- Customer **web** app (mobile-first for v1; web is a later phase — the API
  already supports it).
- ML-based fraud scoring (event hooks provided; model not included).

### Assumptions
- Cloud-native deployment (containers + orchestrator).
- Single primary region at launch; multi-region is a later phase.
- A licensed banking/EMI partner or sandbox provides regulatory cover.

### Constraints
- Must satisfy AML/KYC and data-protection obligations from day one.
- Money movement must be **strongly consistent** within the ledger.
- All privileged actions must be authenticated, authorized, and audited.

---

## 1.4 Stakeholders

| Stakeholder            | Interest / Concern                                    | Influence |
|------------------------|-------------------------------------------------------|-----------|
| Customers              | Fast onboarding, safe funds, reliable transfers       | High      |
| Compliance / AML team  | KYC correctness, audit trail completeness             | High      |
| Operations / Support   | Admin dashboard, ability to investigate & remediate   | High      |
| Engineering            | Maintainability, clear contracts, observability       | High      |
| Security               | AuthN/Z, secrets, least privilege, attack surface     | High      |
| Finance / Treasury     | Ledger accuracy, reconciliation                       | Medium    |
| Executives / Investors | Time to market, cost, regulatory risk                 | High      |
| Regulators / Auditors  | Traceability, controls, data handling                 | High      |

---

## 1.5 Deliverables & Milestones

| Milestone | Deliverable                                              | Phase     |
|-----------|----------------------------------------------------------|-----------|
| M0        | Approved plan, scope, budget (this document)             | Planning  |
| M1        | Requirements baseline + use-case catalog                 | Analysis  |
| M2        | Architecture & detailed design sign-off                  | Design    |
| M3        | Core ledger + accounts/wallets MVP                       | Build     |
| M4        | KYC + RBAC + audit trail                                 | Build     |
| M5        | Gateway, rate limiting, observability stack              | Build     |
| M6        | Security review, pen test, compliance review             | Verify    |
| M7        | Production launch (single region)                        | Deploy    |

---

## 1.6 High-Level Schedule (indicative)

```
Planning   ▓▓
Analysis     ▓▓▓
Design          ▓▓▓
Build               ▓▓▓▓▓▓▓▓▓
Verify                       ▓▓▓
Deploy                          ▓
            |---|---|---|---|---|---|---|---|---|---|  (weeks)
            0   2   4   6   8  10  12  14  16  18  20
```

Estimate: ~18–20 weeks to first production launch with a small, senior team.

---

## 1.7 Team & Roles (RACI summary)

| Function                 | Role on project                          |
|--------------------------|------------------------------------------|
| Product Owner            | Prioritization, scope, sign-off (A)      |
| Tech Lead / Architect    | Architecture, design authority (R/A)     |
| Backend Engineers        | Services, ledger, APIs (R)               |
| Platform / DevOps        | Gateway, CI/CD, observability (R)        |
| Security Engineer        | Threat model, auth, review (R/C)         |
| Compliance Officer       | KYC/AML rules, audit requirements (C/A)  |
| QA Engineer              | Test strategy, automation (R)            |
| SRE                      | SLOs, on-call, dashboards (C)            |

---

## 1.8 Budget / Cost Drivers (qualitative)

- **People:** senior backend + platform + security (largest line item).
- **Infrastructure:** orchestrated compute, managed DB (with PITR), message
  broker, observability stack (Prometheus/Grafana/tracing backend).
- **Third parties:** KYC/identity verification vendor, sanctions/PEP screening.
- **Compliance:** legal, audit, pen testing.
- **Contingency:** 15–20% for regulatory and security unknowns.

---

## 1.9 Risk Register

| ID | Risk                                            | Likelihood | Impact | Mitigation                                                     |
|----|-------------------------------------------------|-----------|--------|----------------------------------------------------------------|
| R1 | Ledger inconsistency / double-spend             | Low       | Critical | Double-entry ledger, DB transactions, idempotency keys, tests |
| R2 | KYC vendor downtime / poor accuracy             | Medium    | High   | Vendor abstraction, async retries, manual-review fallback      |
| R3 | Security breach / token theft                   | Low       | Critical | Short-lived JWT, rotating refresh tokens, RBAC, secrets mgmt   |
| R4 | Compliance gaps surface late                    | Medium    | High   | Compliance officer embedded from Analysis; audit trail first   |
| R5 | Scope creep (cards, lending, multi-region)      | High      | Medium | Strict scope gate; backlog for later phases                    |
| R6 | Observability added too late to debug incidents | Medium    | High   | Metrics/tracing are launch blockers, not nice-to-haves         |
| R7 | Event ordering / duplicate processing           | Medium    | High   | Idempotent consumers, outbox pattern, dedup keys               |
| R8 | Rate-limit misconfiguration (DoS or false block)| Medium    | Medium | Tunable policies, monitoring, gradual rollout                  |

---

## 1.10 Feasibility Assessment

| Dimension     | Assessment                                                                 |
|---------------|----------------------------------------------------------------------------|
| Technical     | Feasible with mature, well-understood tech (containers, RDBMS, broker).     |
| Operational   | Feasible; requires SRE discipline and a staffed compliance function.        |
| Economic      | Justified if onboarding + transaction volume targets are met.               |
| Legal/Regulatory | Feasible **only** with KYC/AML controls and a licensed partner/sandbox.  |
| Schedule      | ~18–20 weeks realistic for MVP with a senior team; aggressive but doable.   |

**Go / No-Go recommendation:** **GO**, conditional on (a) a confirmed
licensed/EMI partner, (b) a named compliance owner, and (c) treating audit
trail + observability as launch-blocking, not optional.

---

## 1.11 Success Criteria (exit gate for Planning)

- [x] Vision, scope (in/out), and assumptions documented and agreed.
- [x] Stakeholders identified with interests and influence.
- [x] Milestones, indicative schedule, and team roles defined.
- [x] Risk register with mitigations established.
- [x] Feasibility assessed; Go/No-Go decision recorded.

➡ Proceed to **Phase 2 — Analysis** (`02-analysis.md`).
