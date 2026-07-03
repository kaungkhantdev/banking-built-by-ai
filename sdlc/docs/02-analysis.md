# Phase 2 — Analysis

> Digital Banking Platform · SDLC Phase 2 of 3
> Status: Requirements Baseline · Owner: Product + Tech Lead + Compliance

---

## 2.1 Purpose

Translate the approved plan into precise, testable requirements: **what** the
system must do (functional), **how well** it must do it (non-functional), the
actors involved, the use cases they perform, the data the system holds, and the
business rules that govern money and identity.

---

## 2.2 Actors

| Actor             | Description                                                        |
|-------------------|--------------------------------------------------------------------|
| Customer          | End user; owns accounts and wallets, initiates transfers.          |
| Admin / Operator  | Staff using the admin dashboard to investigate and remediate.      |
| Compliance Officer| Reviews KYC, sanctions hits, suspicious activity.                  |
| Support Agent     | Limited operator; reads data, opens tickets, cannot move money.    |
| System / Scheduler| Automated jobs (settlement, KYC polling, audit export).           |
| External KYC Vendor | Verifies identity documents and returns a decision.             |
| External Rail (abstracted) | Future external transfer destination.                     |

---

## 2.3 Functional Requirements

### FR-A · Customer Accounts
- FR-A1: Register a customer with unique email/phone.
- FR-A2: Authenticate via credentials, receive JWT access + refresh token.
- FR-A3: View and update profile (subject to KYC lock on key fields).
- FR-A4: Account lifecycle states: `PENDING → ACTIVE → SUSPENDED → CLOSED`.
- FR-A5: Admin can suspend/close accounts (audited, RBAC-gated).

### FR-B · Wallets
- FR-B1: Each customer has one or more wallets.
- FR-B2: Wallet holds a balance in a single currency (multi-currency-ready).
- FR-B3: Balance is **derived from the ledger**, never edited directly.
- FR-B4: Wallet states: `ACTIVE`, `FROZEN`, `CLOSED`.
- FR-B5: Freezing a wallet blocks debits but allows reconciliation reads.

### FR-C · Transactions
- FR-C1: Every money movement creates immutable double-entry ledger records.
- FR-C2: Transaction types: `CREDIT`, `DEBIT`, `HOLD`, `RELEASE`, `REVERSAL`.
- FR-C3: Transactions carry an **idempotency key** to prevent duplicates.
- FR-C4: A transaction is atomic: either fully applied or not at all.
- FR-C5: Insufficient funds → transaction rejected, no partial state.
- FR-C6: Reversal references the original transaction and is itself audited.

### FR-D · Transfers
- FR-D1: Wallet-to-wallet transfer between customers (intra-platform).
- FR-D2: Transfer = paired debit + credit, committed atomically.
- FR-D3: Transfer is rejected if source frozen, KYC-incomplete, or low balance.
- FR-D4: Transfer emits a domain event (`TransferCompleted` / `TransferFailed`).
- FR-D5: External-rail transfers are modeled but stubbed (out of scope v1).

### FR-E · KYC
- FR-E1: Customer submits identity documents.
- FR-E2: KYC states: `NOT_STARTED → PENDING → VERIFIED → REJECTED`.
- FR-E3: Verification is delegated to an external vendor (async).
- FR-E4: Money-out actions require `VERIFIED`; configurable limits below it.
- FR-E5: Sanctions/PEP screening hook on verification.
- FR-E6: Compliance officer can override decision (audited, RBAC-gated).

### FR-F · Audit Logs / Trail
- FR-F1: Every state change records actor, action, before/after, timestamp.
- FR-F2: Audit records are **append-only and immutable**.
- FR-F3: Audit trail is queryable by entity, actor, time range, action.
- FR-F4: Privileged/admin actions are always audited.
- FR-F5: Audit export for regulators (signed, tamper-evident).

### FR-G · Admin Dashboard
- FR-G1: Search customers, wallets, transactions, KYC cases.
- FR-G2: View audit trail per entity.
- FR-G3: Perform RBAC-gated operations (suspend, freeze, reverse, override).
- FR-G4: View platform health (links to Grafana / metrics).

### FR-H · RBAC & Multi-Role
- FR-H1: Roles: `customer`, `support`, `operator`, `compliance`, `admin`,
  `auditor` (read-only).
- FR-H2: Permissions are attached to roles; users may hold multiple roles.
- FR-H3: Every protected endpoint enforces a required permission.
- FR-H4: Privilege escalation attempts are denied and audited.

### FR-I · Platform / Enterprise
- FR-I1: All external traffic enters via an **API Gateway**.
- FR-I2: **Rate limiting** per client/identity/endpoint, configurable.
- FR-I3: Domain changes publish **events** to a broker (event-driven).
- FR-I4: Consumers are **idempotent** (handle duplicates safely).
- FR-I5: Services expose **Prometheus metrics**.
- FR-I6: **Distributed tracing** spans gateway → services → DB/broker.
- FR-I7: **Grafana dashboards** visualize golden signals + business KPIs.

---

## 2.4 Non-Functional Requirements (NFRs)

| ID    | Category        | Requirement                                                                 |
|-------|-----------------|------------------------------------------------------------------------------|
| NFR1  | Security        | JWT access (short TTL) + rotating refresh tokens; TLS everywhere; secrets in a vault. |
| NFR2  | Consistency     | Ledger operations strongly consistent; no double-spend; idempotent writes.  |
| NFR3  | Availability    | 99.9%+ for read APIs; money-movement path designed for graceful degradation.|
| NFR4  | Performance     | p95 API latency < 300 ms for core reads; transfers < 800 ms p95.            |
| NFR5  | Scalability     | Stateless services scale horizontally; broker absorbs spikes.               |
| NFR6  | Auditability    | 100% of state changes captured; audit immutable and exportable.            |
| NFR7  | Observability   | Metrics, logs, traces correlated by trace/request ID.                        |
| NFR8  | Compliance      | KYC/AML enforced; data residency & retention configurable.                  |
| NFR9  | Privacy         | PII encrypted at rest; least-privilege access; right-to-erasure workflow.   |
| NFR10 | Reliability     | At-least-once events + idempotency = effectively-once processing.           |
| NFR11 | Maintainability | Clear service boundaries, versioned APIs, documented contracts.             |
| NFR12 | Recoverability  | DB PITR; RPO ≤ 5 min, RTO ≤ 1 hr for core services.                          |

---

## 2.5 Key Use Cases

### UC-1 · Customer Onboarding (with KYC)
**Actor:** Customer
**Pre:** None
**Flow:**
1. Customer registers (FR-A1).
2. System creates account in `PENDING`, issues JWT (FR-A2).
3. Customer submits KYC documents (FR-E1).
4. System sends to vendor; state `PENDING` (FR-E3).
5. Vendor returns decision; sanctions hook runs (FR-E5).
6. On `VERIFIED`, account → `ACTIVE`, default wallet created (FR-B1).
**Post:** Customer can transact within limits.
**Alt:** `REJECTED` → compliance review queue (FR-E6).

### UC-2 · Wallet-to-Wallet Transfer
**Actor:** Customer
**Pre:** Source `VERIFIED`, `ACTIVE`, sufficient balance.
**Flow:**
1. Customer requests transfer with idempotency key (FR-C3).
2. System validates KYC, wallet state, balance (FR-D3).
3. Ledger writes paired debit+credit in one DB transaction (FR-D2).
4. Emits `TransferCompleted` event (FR-D4).
5. Audit record written (FR-F1).
**Post:** Balances updated; event consumers notified.
**Alt:** Validation fails → reject, emit `TransferFailed`, audit, no balance change.

### UC-3 · Admin Reverses a Transaction
**Actor:** Operator (RBAC `transaction:reverse`)
**Flow:**
1. Operator locates transaction in dashboard (FR-G1).
2. Requests reversal; RBAC checked (FR-H3).
3. System writes compensating ledger entries referencing original (FR-C6).
4. Audit record with actor + reason (FR-F4).
**Post:** Net effect reversed; full trail preserved.

### UC-4 · Compliance Reviews KYC Case
**Actor:** Compliance Officer
**Flow:**
1. Officer opens pending/rejected case.
2. Reviews documents, sanctions hits.
3. Approves or rejects (override) — audited (FR-E6).
**Post:** Account KYC state updated; downstream limits adjusted.

### UC-5 · Auditor Exports Trail
**Actor:** Auditor (read-only)
**Flow:**
1. Auditor queries by entity/time/action (FR-F3).
2. Requests signed export (FR-F5).
**Post:** Tamper-evident export delivered; export action itself audited.

---

## 2.6 Business Rules

- BR1: **A wallet balance can never be negative** (except explicit overdraft
  products, out of scope v1).
- BR2: **Debits require sufficient available balance** (balance minus holds).
- BR3: **Money-out requires KYC = VERIFIED** (sub-limits configurable for lower
  tiers).
- BR4: **Every ledger entry is immutable**; corrections are new compensating
  entries, never edits.
- BR5: **Idempotency key** uniquely identifies a money-movement request for a
  configurable window.
- BR6: **Sum of all ledger entries per closed transaction = 0** (double-entry
  invariant).
- BR7: **Frozen/suspended** entities reject debits but remain readable.
- BR8: **Refresh token rotation:** using a refresh token invalidates it and
  issues a new pair; reuse of a consumed token revokes the session.

---

## 2.7 Data Requirements (conceptual)

Core entities and key attributes (refined into schema in Design):

- **Customer** — id, contact, status, kyc_status, created_at.
- **Wallet** — id, customer_id, currency, status, created_at (balance derived).
- **LedgerEntry** — id, transaction_id, wallet_id, direction, amount, created_at.
- **Transaction** — id, type, status, idempotency_key, metadata, created_at.
- **Transfer** — id, source_wallet, dest_wallet, amount, status, event_ref.
- **KycCase** — id, customer_id, status, vendor_ref, decided_by, decided_at.
- **AuditRecord** — id, actor_id, action, entity, before, after, ts, trace_id.
- **Role / Permission / UserRole** — RBAC mapping tables.
- **RefreshToken** — id, user_id, hash, status, expires_at, rotated_from.

See `03-design.md` §3.4 for the physical data model and relationships.

---

## 2.8 Traceability (requirements → use cases)

| Requirement group | Covered by use cases |
|-------------------|----------------------|
| FR-A, FR-E        | UC-1, UC-4           |
| FR-B, FR-C, FR-D  | UC-2, UC-3           |
| FR-F              | UC-3, UC-5           |
| FR-H (RBAC)       | UC-3, UC-4, UC-5     |
| FR-I (platform)   | Cross-cutting (all)  |

---

## 2.9 Success Criteria (exit gate for Analysis)

- [x] Functional requirements enumerated and grouped.
- [x] NFRs defined with measurable targets.
- [x] Actors, use cases, and business rules documented.
- [x] Conceptual data model and requirement→use-case traceability established.

➡ Proceed to **Phase 3 — Design** (`03-design.md`).
