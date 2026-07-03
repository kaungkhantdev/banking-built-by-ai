# Feature 8 — Audit Trail

**Package:** `com.bank.feature.audit` · **Endpoints:** `/v1/audit`

## Purpose
An append-only record of who did what, when, and what changed — independent of
application logs (which rotate and are mutable). A regulatory and incident-response
requirement.

## Layout
```
audit/
├── web/        AuditController
├── domain/     AuditService + DefaultAuditService, AuditAspect, @Audited
└── persistence/ AuditRecord, AuditRecordRepository
```

## Model
`AuditRecord` — `actor`, `action`, `beforeJson`, `afterJson`, `traceId`, `at`.
Append-only; indexed by actor and action for query.

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/v1/audit?actor=&action=` | `audit:read` | Paged search of the trail (auditor only) |

## Key rules
- **Two ways to record:**
  1. **Declarative** — annotate a method `@Audited(action="...")`; `AuditAspect`
     writes a record after it returns successfully.
  2. **Explicit** — high-stakes paths call `AuditService.write(actor, action,
     before, after)` with rich before/after payloads.
- **Joins the business transaction** — `DefaultAuditService.write` is
  `@Transactional`, so the action and its audit row commit together. No "action
  succeeded but audit lost" gap.
- **traceId** is pulled from the current tracing span (`ApiError.of` helper), tying
  each audit row to logs and the client-visible error envelope.

## Code pointers
- Aspect: `AuditAspect.record`
- Writer: `DefaultAuditService.write`
- Query: `AuditRecordRepository.search`

## Related requirements
FR-7.*, NFR-2.5, NFR-4.4
