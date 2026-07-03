# Non-Functional Requirements — Backend API

How the implementation meets quality attributes. Each item points at the concrete
mechanism in the code.

## NFR-1 Security

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-1.1 | Stateless authentication; no server session. | `SessionCreationPolicy.STATELESS`, JWT in `Authorization: Bearer` |
| NFR-1.2 | Asymmetric token signing (private signs, public verifies). | RS256 via `KeyProvider` + `DefaultJwtService` |
| NFR-1.3 | Credentials at rest are hashed, never reversible. | BCrypt(12) for passwords; SHA-256 for refresh tokens |
| NFR-1.4 | Default-deny authorization; least privilege per role. | `SecurityConfig` + seeded role/permission grants |
| NFR-1.5 | Refresh-token theft is detected and contained. | family revoke on reuse (`revokeSession`) |
| NFR-1.6 | Internal errors never leak to clients. | `GlobalExceptionHandler` returns `INTERNAL` + traceId only; `server.error.include-message: never` |
| NFR-1.7 | Abuse resistance at the application layer. | `RateLimitFilter` (per-principal token bucket) |
| NFR-1.8 | Secrets are externalized (not in source). | env-driven `application.yml`; keys via `JWT_*` / secrets manager; `.gitignore` excludes `*.pem` |

## NFR-2 Reliability & Data Integrity

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-2.1 | Money correctness: no partial debits/credits ever observable. | single `@Transactional` debit+credit+outbox in `DefaultTransferService` |
| NFR-2.2 | Exactly-once money semantics under client retries. | DB-unique `idem_key`; replay on duplicate |
| NFR-2.3 | No lost or phantom events (no dual-write). | transactional outbox + relay |
| NFR-2.4 | At-least-once delivery made effectively-once. | idempotent consumer + `processed_events` |
| NFR-2.5 | Immutable financial history. | append-only `ledger_entries` and `audit_records`; reversals are compensating entries |
| NFR-2.6 | Balance can never silently drift. | balance derived as `SUM(ledger_entries)`, never stored |
| NFR-2.7 | Optimistic concurrency on mutable aggregates. | `@Version` on `BaseAuditEntity` |
| NFR-2.8 | Schema changes are ordered and immutable once shipped. | Flyway `V1..V6`; `ddl-auto: validate` in prod |
| NFR-2.9 | Poison messages are isolated. | dead-letter exchange `bank.events.dlx` |

## NFR-3 Performance & Scalability

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-3.1 | Authorization adds no per-request DB round-trip. | permissions embedded in JWT `perms` claim |
| NFR-3.2 | Stateless app scales horizontally behind a load balancer. | no in-process session; N replicas |
| NFR-3.3 | Hot read path (balance) is a single indexed aggregate query. | `ix_ledger_wallet` + `deriveBalance` |
| NFR-3.4 | Outbox draining is bounded per tick. | `findTop100ByStatusOrderByCreatedAtAsc` |
| NFR-3.5 | Connection/IO not held during slow third-party calls. | KYC vendor call is `@Async`, off the request thread |

## NFR-4 Observability

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-4.1 | Health/readiness probes for orchestration. | Actuator health probes |
| NFR-4.2 | Prometheus-scrapable metrics. | `micrometer-registry-prometheus`, `/actuator/prometheus` |
| NFR-4.3 | Distributed tracing with correlation ids. | Micrometer Tracing (OTel bridge); `traceId` in logs + `ApiError` |
| NFR-4.4 | Every client error is correlatable to server state. | `traceId` shared across envelope, logs, audit records |
| NFR-4.5 | Structured logs carry trace/span ids. | `logback-spring.xml` MDC pattern |

## NFR-5 Maintainability

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-5.1 | Uniform, predictable module shape. | every feature = `web` / `domain` / `persistence` |
| NFR-5.2 | Dependencies point inward; core has no outward coupling. | controllers/repos depend on `domain` interfaces |
| NFR-5.3 | Testability without infrastructure. | service interfaces + `Default*` impls mocked in unit tests |
| NFR-5.4 | One source of truth per cross-cutting concern. | `shared/` (error shape, money, base entity) |
| NFR-5.5 | Self-contained build. | Maven Wrapper (`./mvnw`); H2 for tests, no external infra |

## NFR-6 Portability & Configuration

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-6.1 | Environment-specific config without rebuilds. | Spring profiles `dev` / `prod` / `test` |
| NFR-6.2 | Runs locally with zero external setup for tests. | H2 in-memory; broker auto-startup disabled in test |
| NFR-6.3 | Twelve-factor config via environment variables. | `DB_*`, `RABBITMQ_*`, `JWT_*`, `PORT` |

# NFR-7 Availability

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-7.1 | The service remains available during rolling deployments. | Kubernetes RollingUpdate / Blue-Green deployment |
| NFR-7.2 | Multiple application instances can serve requests concurrently. | Stateless Spring Boot instances behind Load Balancer |
| NFR-7.3 | Application startup failures are detected automatically. | Kubernetes startup probes |
| NFR-7.4 | Unhealthy instances are automatically removed from traffic. | Readiness probe |
| NFR-7.5 | Crashed instances restart automatically. | Kubernetes restart policy |

# NFR-8 Disaster Recovery

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-8.1 | Database backups are performed automatically. | Scheduled PostgreSQL backups |
| NFR-8.2 | Recovery procedures are documented and repeatable. | Disaster Recovery Runbook |
| NFR-8.3 | Backups are encrypted before storage. | AES-256 encrypted backup archives |
| NFR-8.4 | Database restoration is periodically tested. | Recovery drills |
| NFR-8.5 | Critical configuration is backed up separately from application code. | Infrastructure-as-Code + Secret Manager |

# NFR-9 Privacy & Compliance

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-9.1 | Personally identifiable information (PII) is protected. | Encryption at rest |
| NFR-9.2 | Sensitive fields are masked in logs. | Logback masking filters |
| NFR-9.3 | Customer consent is recorded where required. | Consent audit records |
| NFR-9.4 | Personal data deletion complies with retention policies. | Data retention scheduler |
| NFR-9.5 | Audit records are retained according to compliance requirements. | Immutable audit storage |

# NFR-10 Monitoring & Alerting

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-10.1 | Infrastructure metrics are continuously collected. | Prometheus Node Exporter |
| NFR-10.2 | Dashboards visualize application health. | Grafana |
| NFR-10.3 | High error rates trigger alerts. | Alertmanager |
| NFR-10.4 | Slow API responses generate alerts. | Prometheus latency alerts |
| NFR-10.5 | RabbitMQ queue depth is monitored. | RabbitMQ Exporter |

# NFR-11 API Quality

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-11.1 | APIs are versioned. | `/v1/*` endpoints |
| NFR-11.2 | APIs follow REST conventions consistently. | Spring MVC REST Controllers |
| NFR-11.3 | OpenAPI documentation is generated automatically. | springdoc-openapi |
| NFR-11.4 | Pagination is consistent across endpoints. | Pageable abstraction |
| NFR-11.5 | Sorting and filtering follow common conventions. | Query parameters |

# NFR-12 Database

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-12.1 | Foreign keys enforce referential integrity. | PostgreSQL FK constraints |
| NFR-12.2 | Frequently queried columns are indexed. | Flyway index migrations |
| NFR-12.3 | Database transactions use ACID guarantees. | PostgreSQL |
| NFR-12.4 | Soft deletes are avoided for financial records. | Append-only ledger |
| NFR-12.5 | Connection pooling minimizes latency. | HikariCP |

# NFR-13 Deployment

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-13.1 | The application is containerized. | Docker |
| NFR-13.2 | Images are immutable after release. | Versioned Docker images |
| NFR-13.3 | Deployments are automated. | GitHub Actions CI/CD |
| NFR-13.4 | Configuration is injected at runtime. | Environment variables |
| NFR-13.5 | Rollbacks can be performed quickly. | Previous container image deployment |

# NFR-14 Testing

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-14.1 | Business logic is covered by unit tests. | JUnit + Mockito |
| NFR-14.2 | Database integration is verified. | Testcontainers PostgreSQL |
| NFR-14.3 | REST APIs are tested automatically. | MockMvc |
| NFR-14.4 | Message consumers are integration tested. | RabbitMQ Testcontainers |
| NFR-14.5 | Critical financial workflows are regression tested. | CI Pipeline |

# NFR-15 Resilience

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-15.1 | External service failures do not cascade. | Circuit Breaker |
| NFR-15.2 | Temporary failures are retried automatically. | Spring Retry |
| NFR-15.3 | Retry delays increase exponentially. | Exponential Backoff |
| NFR-15.4 | External requests have timeouts. | HTTP Client timeout |
| NFR-15.5 | Bulkheads isolate resource exhaustion. | Resilience4j Bulkhead |

# NFR-16 Audit & Traceability

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-16.1 | Every state-changing operation is auditable. | Audit Aspect |
| NFR-16.2 | Audit records cannot be modified. | Append-only storage |
| NFR-16.3 | Every transaction has a globally unique identifier. | UUID v7 |
| NFR-16.4 | Requests can be traced across services. | OpenTelemetry |
| NFR-16.5 | Business events include correlation IDs. | traceId propagation |

# NFR-17 Performance Targets

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-17.1 | API median response time is under 100 ms under normal load. | Performance testing |
| NFR-17.2 | API 95th percentile response time remains under 300 ms. | Load testing |
| NFR-17.3 | The system supports at least 1,000 concurrent users. | Horizontal scaling |
| NFR-17.4 | Peak throughput exceeds 500 requests/sec. | Load balancer + stateless services |
| NFR-17.5 | Database query plans are periodically reviewed. | PostgreSQL EXPLAIN ANALYZE |

# NFR-18 Documentation

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-18.1 | API documentation is automatically generated. | OpenAPI |
| NFR-18.2 | Architecture is documented. | C4 Model diagrams |
| NFR-18.3 | Database schema is version controlled. | Flyway |
| NFR-18.4 | Deployment procedures are documented. | README + Operations Guide |
| NFR-18.5 | Disaster recovery procedures are documented. | Runbook |

# NFR-19 Configuration Management

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-19.1 | Secrets are rotated periodically. | Secret Manager |
| NFR-19.2 | Configuration changes require no rebuild. | Environment variables |
| NFR-19.3 | Feature flags allow controlled rollout. | Feature Flag Service |
| NFR-19.4 | Configuration changes are auditable. | Version-controlled configuration |
| NFR-19.5 | Production configuration differs from development through profiles only. | Spring Profiles |

# NFR-20 Operational Excellence

| ID | Requirement | Mechanism |
|----|-------------|-----------|
| NFR-20.1 | Zero-downtime deployments are supported. | Rolling deployment |
| NFR-20.2 | All services expose health endpoints. | Spring Boot Actuator |
| NFR-20.3 | Operational metrics are retained historically. | Prometheus TSDB |
| NFR-20.4 | Incidents are supported by centralized logs. | ELK / Loki |
| NFR-20.5 | Service-level objectives (SLOs) are continuously measured. | Grafana + Prometheus |

## Verification status

- `./mvnw clean test` → **10 unit tests pass** (money double-entry + idempotency +
  insufficient-funds; refresh-token reuse detection; KYC state machine; Money util).
- Full compile of all 9 features is clean.
- Runtime startup (`spring-boot:run`) requires PostgreSQL + RabbitMQ; unit tests
  deliberately avoid both so the build is verifiable anywhere.

> **Toolchain note:** built and tested on JDK 25 with Spring Boot 3.3.5 compiled to
> Java 21 bytecode. Surefire passes `-Dnet.bytebuddy.experimental=true` because
> JDK 25 is newer than ByteBuddy's official support matrix; remove once the
> toolchain catches up.
