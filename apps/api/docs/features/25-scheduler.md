# Feature 25 — Scheduler

**Package:** `com.bank.feature.scheduler` · **Endpoints:** `/v1/scheduled-transfers/*`

## Purpose
Allow users to schedule one-off and recurring transfers that execute automatically,
survive application restarts, and retry on failure.

## Layout
```
scheduler/
├── web/        ScheduledTransferController, dto/ (ScheduleRequest, ScheduledTransferView)
├── domain/     SchedulerService + DefaultSchedulerService, RecurrenceRule
└── persistence/ ScheduledTransfer, ScheduledTransferRepository, ScheduledTransferStatus
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/scheduled-transfers` | `transfer:create` | Schedule a transfer |
| GET | `/v1/scheduled-transfers` | `transfer:read` | List scheduled transfers |
| DELETE | `/v1/scheduled-transfers/{id}` | `transfer:create` | Cancel a scheduled transfer |

## Key rules
- Scheduled transfers are stored in `ScheduledTransfer` with a `next_run_at` timestamp.
  A `@Scheduled` poller picks up due jobs and delegates to `DefaultTransferService.transfer`,
  inheriting all existing guards (KYC, limits, fraud, compliance).
- Recurrence rules follow cron-like expressions (daily, weekly, monthly) stored in the
  `recurrence_rule` column; the job advances `next_run_at` after each execution.
- Jobs persist across restarts because they are DB-backed, not in-memory.
- A failed execution writes the error to `ScheduledTransfer.last_error` and retries
  up to a configurable max-attempts before moving to `FAILED` status.
- Cancelling a scheduled transfer marks it `CANCELLED` but does not affect transfers
  already executed.

## Why
Delegating execution to the existing transfer service ensures scheduled transfers
pass through every safety gate (KYC, limits, fraud) exactly as a manual transfer would.

## Related requirements
FR-25.*, FR-5.*
