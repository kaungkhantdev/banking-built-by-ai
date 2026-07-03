# Feature 20 — Limits

**Package:** `com.bank.feature.limits` · **Endpoints:** none (internal gate)

## Purpose
Enforce per-customer daily and monthly transfer limits that vary by customer tier,
and reset automatically at the end of each period.

## Layout
```
limits/
├── domain/     LimitGate + DefaultLimitGate, LimitPolicy
└── persistence/ LimitConfig, LimitConfigRepository, LimitUsage, LimitUsageRepository
```

## How it works
1. `DefaultTransferService` calls `LimitGate.assertWithinLimits(customerId, amount)`
   before executing a transfer.
2. `LimitGate` sums the customer's transfers for the current day and current month
   from `LimitUsage` and compares against the `LimitConfig` for their tier.
3. If either limit would be breached the transfer is rejected with `422 LIMIT_EXCEEDED`.
4. On successful transfer, `LimitUsage` is incremented within the same `@Transactional`
   unit to prevent race conditions under concurrent transfers.
5. A `@Scheduled` job resets daily usage at midnight and monthly usage on the 1st.

## Key rules
- `LimitConfig` is per-tier (e.g., STANDARD, PREMIUM, BUSINESS) and configurable
  in the database without a redeploy.
- Usage is tracked separately from the ledger so historical limits can be audited
  even after ledger entries roll up.
- The reset job is idempotent: re-running it for an already-reset period is a no-op.

## Why
Counting from `LimitUsage` inside the transfer transaction prevents two concurrent
transfers from both reading the same usage figure and both slipping under the limit.

## Related requirements
FR-20.*
