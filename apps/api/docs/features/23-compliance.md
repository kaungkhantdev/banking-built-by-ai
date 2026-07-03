# Feature 23 — Compliance

**Package:** `com.bank.feature.compliance` · **Endpoints:** none (internal gate)

## Purpose
Screen customers and counterparties against sanctions lists and PEP (Politically
Exposed Persons) databases, and inspect transfers for AML (Anti-Money Laundering)
rule violations before money moves.

## Layout
```
compliance/
├── domain/     ComplianceGate + DefaultComplianceGate, SanctionsScreener, AmlInspector
└── persistence/ ComplianceDecision, ComplianceDecisionRepository
```

## How it works
1. **Customer onboarding:** `ComplianceGate.screenCustomer(customer)` is called when a
   customer profile is created. A sanctions or PEP hit moves the customer to
   `SUSPENDED` and writes a `ComplianceDecision` for operator review.
2. **Transfer:** `ComplianceGate.screenTransfer(command)` runs AML rules (structuring
   patterns, counterparty screening) before ledger entries are posted. Violations
   reject the transfer with `422 COMPLIANCE_BLOCK`.
3. Every screening result — pass or fail — is persisted as a `ComplianceDecision`
   with its inputs, rule version, and timestamp.

## Key rules
- Sanctions list data is imported on a schedule and versioned; the rule version
  is recorded with each decision so historical decisions can be explained.
- All compliance decisions are auditable and immutable once written.
- PEP status does not automatically block a customer; it triggers enhanced due
  diligence (EDD) review by a compliance officer.

## Why
Recording every screening decision (including passes) provides a complete audit
trail demonstrating that controls were applied, which regulators require.

## Related requirements
FR-23.*, FR-7.1
