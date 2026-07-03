# Feature 29 — Observability

**Package:** `com.bank.config` (cross-cutting) · **Endpoints:** `/actuator/*`

## Purpose
Ensure every request carries a trace ID, emit business metrics to Prometheus, support
distributed tracing, and log slow requests for performance investigation.

## Layout
```
config/
├── TraceIdFilter         — injects / propagates X-Trace-Id on every request
├── SlowRequestFilter     — logs requests exceeding a configurable threshold
└── MetricsConfig         — registers custom Micrometer meters
```

## How it works
- **Trace ID:** `TraceIdFilter` reads `X-Trace-Id` from the incoming request (or
  generates a UUID if absent), stores it in MDC, and echoes it in the response header.
  All log lines in the request thread carry `traceId` automatically.
- **Distributed tracing:** OpenTelemetry auto-instrumentation propagates the trace
  context (W3C `traceparent`) across service boundaries and RabbitMQ messages.
- **Prometheus metrics:** Micrometer exports JVM, HTTP, and custom business meters
  (e.g., `transfers.completed`, `kyc.verified`) via `/actuator/prometheus`.
- **Slow request logging:** `SlowRequestFilter` records method, path, status, and
  duration for requests that exceed the threshold (e.g., 1 s) at `WARN` level.

## Key rules
- The trace ID is always present in error responses (`ApiError.traceId`) so callers
  can correlate a failed request with server logs.
- Business metrics must not carry PII in label values (e.g., use `currency`, not
  `user_id`, as a label).
- Actuator endpoints are restricted: `/actuator/prometheus` and `/actuator/health`
  are open; all others require `admin:read`.

## Why
Embedding the trace ID in every error response closes the loop between a user-facing
error and the corresponding server log line without requiring the user to know what
time the error occurred.

## Related requirements
FR-29.*, FR-9.1, FR-9.4
