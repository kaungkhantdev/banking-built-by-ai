# Feature 22 — Fraud Detection

**Package:** `com.bank.feature.fraud` · **Endpoints:** none (internal engine)

## Purpose
Score each transfer for fraud risk using velocity rules and value thresholds, flag
suspicious transfers for manual review, and emit audit events for every fraud decision.

## Layout
```
fraud/
├── domain/     FraudEngine + DefaultFraudEngine, RiskScore, VelocityRule
└── persistence/ FraudAlert, FraudAlertRepository, FraudRuleConfig, FraudRuleConfigRepository
```

## How it works
1. `DefaultTransferService` calls `FraudEngine.assess(command)` after the KYC and
   limit checks but before posting ledger entries.
2. `FraudEngine` runs a configurable rule set:
   - **High-value rule:** transfers above a threshold receive a base risk score.
   - **Velocity rule:** N+ transfers within a rolling window from the same sender
     increment the score.
3. If the aggregate score exceeds the block threshold the transfer is rejected with
   `422 TRANSFER_FLAGGED_FOR_REVIEW` and a `FraudAlert` is created.
4. If the score is in the review band (warn, not block), the transfer proceeds but a
   `FraudAlert` with `PENDING_REVIEW` status is created for operator review.
5. Every assessment result writes an audit event.

## Key rules
- Rule thresholds are stored in `FraudRuleConfig` and can be updated without a redeploy.
- Manual approval of a flagged transfer advances the `FraudAlert` to `APPROVED` and
  re-executes the transfer.
- Fraud assessment is synchronous and part of the transfer transaction so a flagged
  transfer can never silently complete.

## Why
Running fraud assessment inside the transfer transaction means there is no window
between assessment and execution during which a second transfer could slip through
on a borderline score.

## Related requirements
FR-22.*, FR-7.1
