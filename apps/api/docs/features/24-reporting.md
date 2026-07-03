# Feature 24 — Reporting

**Package:** `com.bank.feature.reporting` · **Endpoints:** `/v1/reports/*`

## Purpose
Generate daily transaction reports and financial summaries, exportable as CSV or
Excel, with filtering by customer, currency, and date range.

## Layout
```
reporting/
├── web/        ReportController, dto/ (ReportRequest, ReportView)
├── domain/     ReportService + DefaultReportService, ReportRenderer
└── persistence/ ReportRecord, ReportRecordRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/reports` | `report:generate` | Request a report for a period |
| GET | `/v1/reports` | `report:read` | List generated reports |
| GET | `/v1/reports/{id}` | `report:read` | Download a report (CSV or Excel) |

## Key rules
- Report generation is asynchronous (`@Async`); the `POST` returns `202 Accepted`.
  The client polls or receives a notification (Feature 13) when the report is ready.
- Supported formats: `CSV` and `XLSX`. Format is chosen via an `Accept` header or
  request parameter.
- Filters: `customerId`, `currency`, `from`, `to` (ISO-8601 dates).
- Generated files are stored in object storage (Feature 27) and linked from
  `ReportRecord`. Re-requesting the same parameters returns the stored file.
- Daily summaries include: total credits, total debits, net flow, transaction count,
  fee revenue — all in `NUMERIC(19,4)`.

## Why
Generating reports asynchronously prevents long-running aggregation queries from
blocking API threads and allows large date ranges without timeout risk.

## Related requirements
FR-24.*
