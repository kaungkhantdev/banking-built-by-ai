# Functional Requirements — Backend API

Each requirement is traced to the code that satisfies it. IDs are referenced from
the feature docs. "Permission" is the authority string enforced by
`@PreAuthorize("hasAuthority('...')")` and seeded in `V6__seed_rbac.sql`.

## FR-1 Authentication & Session

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-1.1 | A visitor can register with email + password (min 8 chars). | `POST /v1/auth/register` → `DefaultAuthService.register` |
| FR-1.2 | Passwords are stored only as BCrypt hashes (never plaintext). | `SecurityConfig.passwordEncoder` (BCrypt strength 12) |
| FR-1.3 | A user can log in and receive an access JWT + refresh token. | `POST /v1/auth/login` → `DefaultAuthService.login` |
| FR-1.4 | Access tokens are short-lived (default 10 min), RS256-signed. | `DefaultJwtService.issueAccessToken` |
| FR-1.5 | A refresh token can be exchanged for a new pair (rotation). | `POST /v1/auth/refresh` → `DefaultAuthService.refresh` |
| FR-1.6 | Reuse of a rotated/revoked refresh token revokes the whole session family. | `DefaultAuthService.refresh` + `RefreshTokenRepository.revokeSession` |
| FR-1.7 | Refresh tokens are persisted only as SHA-256 hashes. | `DefaultAuthService.issuePair` (`Tokens.sha256Hex`) |

## FR-2 Authorization (RBAC)

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-2.1 | Every protected endpoint is default-deny. | `SecurityConfig` (`anyRequest().authenticated()`) |
| FR-2.2 | Fine-grained permissions gate each operation. | `@PreAuthorize("hasAuthority('...')")` on controllers |
| FR-2.3 | Permissions are carried in the JWT `perms` claim — no per-request DB hit. | `DefaultJwtService` + `JwtAuthFilter` |
| FR-2.4 | An operator can assign a role to a user. | `POST /v1/admin/users/{id}/roles` (`user:assign-role`) |
| FR-2.5 | Effective permissions are resolved from user → roles → permissions. | `DefaultPermissionService.permissionsFor` |

## FR-3 Accounts

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-3.1 | An account can be opened (starts PENDING). | `POST /v1/accounts` (`account:create`) |
| FR-3.2 | An account can be activated PENDING → ACTIVE. | `POST /v1/accounts/{id}/activate` (`account:manage`) |
| FR-3.3 | Illegal lifecycle transitions are rejected. | `DefaultAccountService.activate` (409) |

## FR-4 Wallets

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-4.1 | A currency wallet can be opened under an account (one per currency). | `POST /v1/wallets` (`wallet:create`); unique `(account_id, currency)` |
| FR-4.2 | A wallet balance can be read, derived from the ledger. | `GET /v1/wallets/{id}/balance` (`wallet:read`) |
| FR-4.3 | A wallet can be frozen (blocks money-out, deletes nothing). | `POST /v1/wallets/{id}/freeze` (`wallet:manage`) |
| FR-4.4 | Balance is never stored as a column. | `DefaultWalletService.balance` → `LedgerService.balanceOf` |

## FR-5 Ledger, Transactions & Transfers

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-5.1 | Money moves as a balanced double entry (debit + credit). | `DefaultLedgerService.postDoubleEntry` |
| FR-5.2 | A transfer commits in a single ACID transaction. | `DefaultTransferService.transfer` (`@Transactional`) |
| FR-5.3 | Money POSTs are idempotent via a client `Idempotency-Key`. | `TransferController.create` + unique `idem_key` |
| FR-5.4 | A retried transfer replays the original result (no double-charge). | `DefaultTransferService.transfer` (DataIntegrityViolation → replay) |
| FR-5.5 | Insufficient funds is rejected before any ledger write. | `DefaultTransferService.transfer` (422) |
| FR-5.6 | Cross-currency and same-wallet transfers are rejected. | `DefaultTransferService.transfer` (422) |
| FR-5.7 | A transaction can be reversed with compensating entries, never deletes. | `POST /v1/transfers/{id}/reverse` (`transaction:reverse`) |
| FR-5.8 | The ledger is append-only. | `LedgerEntry` (no update/delete); SQL grants note in `V2` |
| FR-5.9 | Amounts use fixed-scale decimals (NUMERIC(19,4)), never floats. | `Money` + `LedgerEntry.amount` |

## FR-6 KYC

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-6.1 | A KYC case is opened per account. | `POST /v1/kyc` (`kyc:submit`) |
| FR-6.2 | Documents can be submitted; verification runs asynchronously. | `POST /v1/kyc/{accountId}/documents`; `@Async orchestrateAsync` |
| FR-6.3 | The case advances through a guarded state machine. | `KycCase.transitionTo` (illegal jumps → 409) |
| FR-6.4 | Money-out is blocked unless the sender's KYC is VERIFIED. | `KycGate.assertMoneyOutAllowed` (called by transfers) |
| FR-6.5 | KYC status can be queried. | `GET /v1/kyc/{accountId}/status` (`kyc:read`) |

## FR-7 Audit Trail

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-7.1 | Privileged actions write an append-only audit record. | `@Audited` + `AuditAspect`; `DefaultAuditService.write` |
| FR-7.2 | Each record captures actor, action, before/after, traceId, timestamp. | `AuditRecord` |
| FR-7.3 | Audit writes join the business transaction (commit together). | `DefaultAuditService.write` (`@Transactional`) |
| FR-7.4 | Auditors can query the trail by actor/action. | `GET /v1/audit` (`audit:read`) |

## FR-8 Event-Driven Side-Effects (Outbox)

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-8.1 | Domain events are written to an outbox inside the business transaction. | `OutboxWriter.write` (`MANDATORY` propagation) |
| FR-8.2 | A relay publishes outbox rows to RabbitMQ after commit. | `OutboxRelay.publishBatch` (`@Scheduled`) |
| FR-8.3 | Each event carries its id so consumers can dedup. | `OutboxRelay` sets message id |
| FR-8.4 | Consumers are idempotent (at-least-once → effectively-once). | `TransferNotificationConsumer` + `processed_events` |
| FR-8.5 | Money is never moved by an event — only side-effects. | events carry notifications/projections only |

## FR-9 API Concerns

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-9.1 | All errors use one envelope `{code, message, traceId}`. | `ApiError` + `GlobalExceptionHandler` |
| FR-9.2 | Request bodies are validated; failures return 400 with field detail. | Bean Validation + handler |
| FR-9.3 | Per-principal rate limiting protects the API (defense in depth). | `RateLimitFilter` (100 req/min, Bucket4j) |
| FR-9.4 | Health, info, and Prometheus metrics are exposed. | Actuator (`/actuator/*`) |

## FR-10 Customer Management

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-10.1 | A customer profile can be created with personal information. | — |
| FR-10.2 | A customer can own multiple accounts. | — |
| FR-10.3 | Customer information can be updated with audit history. | — |
| FR-10.4 | Customer status can be ACTIVE, SUSPENDED, CLOSED. | — |
| FR-10.5 | Customer search supports pagination and filtering. | — |

## FR-11 Beneficiaries

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-11.1 | A user can save transfer beneficiaries. | — |
| FR-11.2 | Duplicate beneficiaries are rejected. | — |
| FR-11.3 | Beneficiaries can be updated or deleted. | — |
| FR-11.4 | Transfers may reference saved beneficiaries. | — |

## FR-12 Transaction History

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-12.1 | Users can retrieve transaction history. | — |
| FR-12.2 | History supports pagination. | — |
| FR-12.3 | History supports filtering by date, type, status, currency. | — |
| FR-12.4 | Transactions can be exported as CSV. | — |
| FR-12.5 | Transactions expose running balance at transaction time. | — |

## FR-13 Notifications

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-13.1 | Users receive email after successful transfers. | — |
| FR-13.2 | Users receive notification when KYC status changes. | — |
| FR-13.3 | Users receive notification when password changes. | — |
| FR-13.4 | Notification delivery failures are retried. | — |
| FR-13.5 | Notification templates are configurable. | — |

## FR-14 Password & Security

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-14.1 | Users can change password. | — |
| FR-14.2 | Password reset uses one-time expiring tokens. | — |
| FR-14.3 | Password history prevents reuse of recent passwords. | — |
| FR-14.4 | Failed logins trigger temporary account lock. | — |
| FR-14.5 | Login attempts are audited. | — |

## FR-15 Multi-Factor Authentication

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-15.1 | Users can enable MFA. | — |
| FR-15.2 | TOTP authenticator apps are supported. | — |
| FR-15.3 | Recovery codes are generated. | — |
| FR-15.4 | Sensitive operations require MFA re-authentication. | — |

## FR-16 Device & Session Management

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-16.1 | Users can view active sessions. | — |
| FR-16.2 | Users can revoke individual sessions. | — |
| FR-16.3 | Users can revoke all sessions except current. | — |
| FR-16.4 | New device logins are recorded. | — |
| FR-16.5 | Device fingerprint is stored for security monitoring. | — |

## FR-17 Account Statements

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-17.1 | Monthly account statements can be generated. | — |
| FR-17.2 | Statements are immutable PDFs. | — |
| FR-17.3 | Users can download historical statements. | — |
| FR-17.4 | Statement generation runs asynchronously. | — |

## FR-18 Currency & Exchange

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-18.1 | Exchange rates are imported automatically. | — |
| FR-18.2 | Historical exchange rates are preserved. | — |
| FR-18.3 | Wallet conversion calculates fees before execution. | — |
| FR-18.4 | Exchange operations are atomic. | — |

## FR-19 Fees

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-19.1 | Transfer fees are configurable. | — |
| FR-19.2 | Fee calculation occurs before execution. | — |
| FR-19.3 | Fee transactions generate separate ledger entries. | — |
| FR-19.4 | Promotional fee waivers are supported. | — |

## FR-20 Limits

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-20.1 | Daily transfer limits are enforced. | — |
| FR-20.2 | Monthly transfer limits are enforced. | — |
| FR-20.3 | Limits vary by customer tier. | — |
| FR-20.4 | Limit usage resets automatically. | — |

## FR-21 Admin Console

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-21.1 | Administrators can search customers. | — |
| FR-21.2 | Administrators can freeze accounts. | — |
| FR-21.3 | Administrators can freeze wallets. | — |
| FR-21.4 | Administrators can unlock locked users. | — |
| FR-21.5 | Administrator actions are audited. | — |

## FR-22 Fraud Detection

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-22.1 | High-value transfers are flagged. | — |
| FR-22.2 | Velocity rules detect suspicious activity. | — |
| FR-22.3 | Risk scores are calculated per transfer. | — |
| FR-22.4 | Suspicious transfers require manual approval. | — |
| FR-22.5 | Fraud alerts generate audit events. | — |

## FR-23 Compliance

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-23.1 | Customers are screened against sanctions lists. | — |
| FR-23.2 | Politically Exposed Persons (PEP) checks are supported. | — |
| FR-23.3 | AML rules inspect suspicious transfers. | — |
| FR-23.4 | Compliance decisions are auditable. | — |

## FR-24 Reporting

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-24.1 | Daily transaction reports are generated. | — |
| FR-24.2 | Reports support CSV and Excel export. | — |
| FR-24.3 | Financial summaries are generated per day. | — |
| FR-24.4 | Reports support filtering by customer, currency, and date. | — |

## FR-25 Scheduler

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-25.1 | Scheduled transfers are supported. | — |
| FR-25.2 | Recurring transfers are supported. | — |
| FR-25.3 | Scheduled jobs survive application restart. | — |
| FR-25.4 | Failed scheduled jobs are retried. | — |

## FR-26 Webhooks

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-26.1 | Third-party systems can register webhook endpoints. | — |
| FR-26.2 | Webhook payloads are signed. | — |
| FR-26.3 | Failed deliveries are retried with exponential backoff. | — |
| FR-26.4 | Delivery history is retained. | — |

## FR-27 File Storage

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-27.1 | KYC documents are stored in object storage. | — |
| FR-27.2 | File uploads are virus scanned. | — |
| FR-27.3 | Files are encrypted at rest. | — |
| FR-27.4 | Temporary upload URLs expire automatically. | — |

## FR-28 Search

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-28.1 | Customers are searchable by name, email, or phone. | — |
| FR-28.2 | Transactions are searchable by reference number. | — |
| FR-28.3 | Search supports pagination and sorting. | — |

## FR-29 Observability

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-29.1 | Every request carries a trace ID. | — |
| FR-29.2 | Distributed tracing is supported. | — |
| FR-29.3 | Business metrics are exported to Prometheus. | — |
| FR-29.4 | Slow requests are logged. | — |

## FR-30 Operational Resilience

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-30.1 | External service calls use configurable timeouts. | — |
| FR-30.2 | Circuit breakers protect dependent services. | — |
| FR-30.3 | Retry policies are configurable per integration. | — |
| FR-30.4 | Bulkheads isolate downstream failures. | — |