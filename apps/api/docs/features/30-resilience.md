# Feature 30 — Operational Resilience

**Package:** `com.bank.config` (cross-cutting) · **Endpoints:** none

## Purpose
Protect the application from cascading failures caused by slow or unavailable
downstream services using timeouts, circuit breakers, retry policies, and bulkheads.

## Layout
```
config/
└── ResilienceConfig      — Resilience4j circuit breakers, bulkheads, retry registries

integration/
└── (per-client)          — each external client applies its own named instance
```

## How it works
- **Timeouts:** every external HTTP client (FX rate provider, sanctions API, email
  service, etc.) is configured with a connection timeout and a read timeout.
- **Circuit breakers:** Resilience4j circuit breakers wrap each integration. After a
  configurable failure-rate threshold is exceeded the breaker opens and requests fail
  fast with a `503` instead of queuing behind a slow dependency.
- **Retries:** transient failures (network glitch, `503`) are retried a fixed number
  of times with exponential back-off before the circuit breaker counts them.
- **Bulkheads:** thread-pool bulkheads isolate each downstream client so a slow
  dependency (e.g., virus scanner) cannot exhaust the common thread pool and
  affect unrelated operations.

## Key rules
- Timeout values, failure thresholds, and retry counts are configurable per integration
  via `application.yml` without a code change.
- Circuit breaker state transitions (open/close/half-open) are logged and emitted
  as Micrometer events for alerting.
- Bulkhead rejection returns `503 SERVICE_UNAVAILABLE` wrapped in the standard
  `ApiError` envelope so clients handle it consistently.

## Why
Without bulkheads, a single slow external call can exhaust all available threads,
making every other endpoint appear unresponsive even though they have no dependency
on the slow service.

## Related requirements
FR-30.*
