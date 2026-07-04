# Functional Requirements — Backend API

Each requirement is traced to the code that satisfies it. IDs are referenced from
the feature docs. "Permission" is the authority string enforced by
`@PreAuthorize("hasAuthority('...')")` and seeded in `V6__seed_rbac.sql` /
`V24__rbac_ext.sql` / `V29__fr_completion.sql`.

**Status legend** (in the `Implemented by` column):
- ✅ implemented and wired end-to-end
- ⚠️ partially implemented — persistence/API exists but a rule is stubbed or not enforced
- ❌ not wired — no code satisfies it yet

All FR-1 through FR-30 are now wired (✅). External integrations (FX feed, virus
scanner, sanctions/PEP lists, trace collector) sit behind seams with a local/stub
default — swap the bean or set config for production, exactly like `KycVendorClient`.

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
| FR-5.7 | A transaction can be reversed with compensating entries, never deletes. | `POST /v1/transfers/{id}/reverse` → `DefaultTransferService.reverse` |
| FR-5.8 | The ledger is append-only. | `LedgerEntry` (no update/delete); SQL grants note in `V2` |
| FR-5.9 | Amounts use fixed-scale decimals (NUMERIC(19,4)), never floats. | `Money` + `LedgerEntry.amount` |

## FR-6 KYC

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-6.1 | A KYC case is opened per account. | `POST /v1/kyc` (`kyc:submit`) |
| FR-6.2 | Documents can be submitted; verification runs asynchronously. | `POST /v1/kyc/{accountId}/documents`; `@Async orchestrateAsync` |
| FR-6.3 | The case advances through a guarded state machine. | `KycCase.transitionTo` (illegal jumps → 409) |
| FR-6.4 | Money-out is blocked unless the sender's KYC is VERIFIED. | `KycGate.assertMoneyOutAllowed` (transfer step 3) |
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
| FR-10.1 | A customer profile can be created with personal information. | ✅ `POST /v1/customers` (`customer:create`) → `DefaultCustomerService.create` |
| FR-10.2 | A customer can own multiple accounts. | ✅ `Customer` / `Account` model (wallets hang off an account) |
| FR-10.3 | Customer information can be updated with audit history. | ✅ `PATCH /v1/customers/{id}` (`customer:update`); `@Audited` |
| FR-10.4 | Customer status can be ACTIVE, SUSPENDED, CLOSED. | ✅ `CustomerStatus`; `POST /v1/customers/{id}/suspend` \| `/close` |
| FR-10.5 | Customer search supports pagination and filtering. | ✅ `GET /v1/customers` (`customer:read`, `Pageable`) → `listCustomers` |

## FR-11 Beneficiaries

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-11.1 | A user can save transfer beneficiaries. | ✅ `POST /v1/beneficiaries` (`beneficiary:create`) |
| FR-11.2 | Duplicate beneficiaries are rejected. | ✅ unique constraint (`V9`) in `DefaultBeneficiaryService` |
| FR-11.3 | Beneficiaries can be updated or deleted. | ✅ `PATCH` / `DELETE /v1/beneficiaries/{id}` |
| FR-11.4 | Transfers may reference saved beneficiaries. | ✅ `TransferRequest.beneficiaryId` → `BeneficiaryService.resolveDestinationWallet` (in `TransferController`) |

## FR-12 Transaction History

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-12.1 | Users can retrieve transaction history. | ✅ `GET /v1/wallets/{id}/transactions` → `DefaultHistoryService.listForWallet` |
| FR-12.2 | History supports pagination. | ✅ `Pageable` |
| FR-12.3 | History supports filtering by date, type, status, currency. | ✅ `LedgerEntryRepository.search` (direction/currency/from/to params) |
| FR-12.4 | Transactions can be exported as CSV. | ✅ `GET /v1/wallets/{id}/transactions/export` → `exportCsv` |
| FR-12.5 | Transactions expose running balance at transaction time. | ✅ `LedgerEntryRepository.signedSumAsOf` → `TransactionView.runningBalance` |

## FR-13 Notifications

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-13.1 | Users receive email after successful transfers. | ✅ `TransferNotificationHandler` (`transfer.completed`) |
| FR-13.2 | Users receive notification when KYC status changes. | ✅ `kyc.status-changed` event → `KycNotificationHandler` |
| FR-13.3 | Users receive notification when password changes. | ✅ `password.changed` event → `PasswordNotificationHandler` |
| FR-13.4 | Notification delivery failures are retried. | ✅ `DefaultNotificationService.deliverWithRetry` (3 attempts) |
| FR-13.5 | Notification templates are configurable. | ✅ `NotificationTemplate` store + `{{var}}` rendering (seeded in `V29`) |

## FR-14 Password & Security

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-14.1 | Users can change password. | ✅ `POST /v1/auth/password/change` → `DefaultPasswordService.changePassword` |
| FR-14.2 | Password reset uses one-time expiring tokens. | ✅ `POST /v1/auth/password/reset/init` + `/reset/confirm`; `PasswordResetToken` |
| FR-14.3 | Password history prevents reuse of recent passwords. | ✅ `PasswordHistory` + `PasswordHistoryRepository` |
| FR-14.4 | Failed logins trigger temporary account lock. | ✅ `User.recordFailedLogin` / `isLocked` (`DefaultAuthService.login`) |
| FR-14.5 | Login attempts are audited. | ✅ `DefaultAuthService.login` writes `auth:login-*` audit records |

## FR-15 Multi-Factor Authentication

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-15.1 | Users can enable MFA. | ✅ `POST /v1/mfa/enroll` + `/enroll/confirm` → `DefaultMfaService` |
| FR-15.2 | TOTP authenticator apps are supported. | ✅ RFC 6238 TOTP in `DefaultMfaService.validateTotp` |
| FR-15.3 | Recovery codes are generated. | ✅ `RecoveryCode`; `GET`/`POST /v1/mfa/recovery-codes[/regenerate]` |
| FR-15.4 | Sensitive operations require MFA re-authentication. | ✅ `MfaService.assertStepUp` gating transfer reverse (`X-MFA-Code`) |

## FR-16 Device & Session Management

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-16.1 | Users can view active sessions. | ✅ `GET /v1/sessions` → `listSessions` |
| FR-16.2 | Users can revoke individual sessions. | ✅ `DELETE /v1/sessions/{sessionId}` → `revokeSession` |
| FR-16.3 | Users can revoke all sessions except current. | ✅ `DELETE /v1/sessions` → `revokeAllSessions` |
| FR-16.4 | New device logins are recorded. | ✅ `DeviceRecord` (`loggedInAt`) |
| FR-16.5 | Device fingerprint is stored for security monitoring. | ✅ `DeviceRecord.userAgent` / `ipAddress` |

## FR-17 Account Statements

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-17.1 | Monthly account statements can be generated. | ✅ `POST /v1/accounts/{id}/statements` (`account:manage`) |
| FR-17.2 | Statements are immutable PDFs. | ✅ `SimplePdf` render + SHA-256 digest seal; verified on download |
| FR-17.3 | Users can download historical statements. | ✅ `GET .../statements` + `/{statementId}` (`account:read`) |
| FR-17.4 | Statement generation runs asynchronously. | ✅ `@Async` in `DefaultStatementService` |

## FR-18 Currency & Exchange

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-18.1 | Exchange rates are imported automatically. | ✅ `ExchangeRateImporter` (`@Scheduled`) via `RateProvider` seam |
| FR-18.2 | Historical exchange rates are preserved. | ✅ `ExchangeRate.effectiveAt` (append-only rows) |
| FR-18.3 | Wallet conversion calculates fees before execution. | ✅ `POST /v1/exchange/quote` → `ExchangeService.quote` |
| FR-18.4 | Exchange operations are atomic. | ✅ `DefaultExchangeService.execute` (`@Transactional`) |

## FR-19 Fees

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-19.1 | Transfer fees are configurable. | ✅ `FeeConfig` / `FeeConfigRepository` (`V15`) |
| FR-19.2 | Fee calculation occurs before execution. | ✅ `FeeEngine.calculate` (transfer step 6) |
| FR-19.3 | Fee transactions generate separate ledger entries. | ✅ fee posted as its own double entry → `systemFeeWalletId` |
| FR-19.4 | Promotional fee waivers are supported. | ✅ `WaiverRecord` + `FeeEngine` waiver check |

## FR-20 Limits

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-20.1 | Daily transfer limits are enforced. | ✅ `LimitGate.assertWithinLimitsAndRecord`; `LimitConfig(DAILY)` |
| FR-20.2 | Monthly transfer limits are enforced. | ✅ `LimitConfig(MONTHLY)` |
| FR-20.3 | Limits vary by customer tier. | ✅ `LimitConfig.customerTier` |
| FR-20.4 | Limit usage resets automatically. | ✅ period-key bucketing + `LimitMaintenance.purgeStaleUsage` (`@Scheduled`) |

## FR-21 Admin Console

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-21.1 | Administrators can search customers. | ✅ `GET /v1/admin/customers` (`admin:read`) |
| FR-21.2 | Administrators can freeze accounts. | ✅ `POST /v1/admin/accounts/{id}/freeze` → `AdminService` (`@Audited`) |
| FR-21.3 | Administrators can freeze wallets. | ✅ `POST /v1/admin/wallets/{id}/freeze` → `AdminService` (`@Audited`) |
| FR-21.4 | Administrators can unlock locked users. | ✅ `POST /v1/admin/users/{id}/unlock` → `AdminService.unlockUser` (`@Audited`) |
| FR-21.5 | Administrator actions are audited. | ✅ `DefaultAdminService` methods `@Audited(admin:*)` |

## FR-22 Fraud Detection

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-22.1 | High-value transfers are flagged. | ✅ `FraudEngine.assess` vs `FraudRuleConfig.thresholdValue` |
| FR-22.2 | Velocity rules detect suspicious activity. | ✅ `FraudRuleConfig.windowMinutes` / `maxCount` |
| FR-22.3 | Risk scores are calculated per transfer. | ✅ `FraudAlert.riskScore` |
| FR-22.4 | Suspicious transfers require manual approval. | ✅ high risk → held (422) + `POST /v1/fraud/alerts/{id}/approve\|dismiss` |
| FR-22.5 | Fraud alerts generate audit events. | ✅ `FraudEngine` emits `fraud.alert.*`; approvals `@Audited` |

## FR-23 Compliance

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-23.1 | Customers are screened against sanctions lists. | ✅ `ComplianceGate.screenSanctions` (config watchlist) → `ComplianceDecision` |
| FR-23.2 | Politically Exposed Persons (PEP) checks are supported. | ✅ `ComplianceGate.screenPep` → `PEP` decision (EDD) |
| FR-23.3 | AML rules inspect suspicious transfers. | ✅ `ComplianceGate.screenTransfer` (transfer step 2) |
| FR-23.4 | Compliance decisions are auditable. | ✅ `ComplianceDecision` persisted (`ruleVersion`, `decision`) |

## FR-24 Reporting

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-24.1 | Daily transaction reports are generated. | ✅ `POST /v1/reports` (`report:generate`) → `DefaultReportService.request` |
| FR-24.2 | Reports support CSV and Excel export. | ✅ `download` renders CSV or SpreadsheetML per `format` |
| FR-24.3 | Financial summaries are generated per day. | ✅ `reportType` selects summary reports |
| FR-24.4 | Reports support filtering by customer, currency, and date. | ✅ `ReportRecord.fromDate` / `toDate` (customer/currency params) |

## FR-25 Scheduler

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-25.1 | Scheduled transfers are supported. | ✅ `POST /v1/scheduled-transfers` (`transfer:create`) |
| FR-25.2 | Recurring transfers are supported. | ✅ `ScheduledTransfer.recurrenceRule` |
| FR-25.3 | Scheduled jobs survive application restart. | ✅ persisted `scheduled_transfers` (`V21`); `@Scheduled runDueTransfers` |
| FR-25.4 | Failed scheduled jobs are retried. | ✅ `attemptCount`/`maxAttempts`; retried each poll until FAILED |

## FR-26 Webhooks

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-26.1 | Third-party systems can register webhook endpoints. | ✅ `POST /v1/webhooks` (`webhook:manage`) |
| FR-26.2 | Webhook payloads are signed. | ✅ `WebhookDispatcher` HMAC-SHA256 `X-Signature`; secret AES-encrypted at rest |
| FR-26.3 | Failed deliveries are retried with exponential backoff. | ✅ `WebhookDispatcher` + `WebhookDelivery.recordFailure` (2^n backoff) |
| FR-26.4 | Delivery history is retained. | ✅ `webhook_deliveries`; `GET /v1/webhooks/{id}/deliveries` |

## FR-27 File Storage

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-27.1 | KYC documents are stored in object storage. | ✅ `POST /v1/files/upload-url` + `/{id}/content`; `FileMeta.storageKey` |
| FR-27.2 | File uploads are virus scanned. | ✅ `VirusScanner` seam (`StubVirusScanner`, EICAR) in `uploadContent` |
| FR-27.3 | Files are encrypted at rest. | ✅ `Aes` (AES-256-GCM) → `FileMeta.contentEncrypted` |
| FR-27.4 | Temporary upload URLs expire automatically. | ✅ `FileMeta.uploadExpiresAt` enforced in `uploadContent` (410) |

## FR-28 Search

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-28.1 | Customers are searchable by name, email, or phone. | ✅ `GET /v1/search/customers` → `searchCustomers` |
| FR-28.2 | Transactions are searchable by reference number. | ✅ `GET /v1/search/transactions` → `searchTransactions` |
| FR-28.3 | Search supports pagination and sorting. | ✅ `Pageable` |

## FR-29 Observability

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-29.1 | Every request carries a trace ID. | ✅ `TraceIdFilter` (MDC + `X-Trace-Id` response header) |
| FR-29.2 | Distributed tracing is supported. | ✅ Micrometer OTel bridge, W3C propagation; `TraceIdFilter` sources span id |
| FR-29.3 | Business metrics are exported to Prometheus. | ✅ `MetricsConfig` (`transfers.completed`, `kyc.verified`, `fraud.alerts.created`) |
| FR-29.4 | Slow requests are logged. | ✅ `SlowRequestFilter` |

## FR-30 Operational Resilience

| ID | Requirement | Implemented by |
|----|-------------|----------------|
| FR-30.1 | External service calls use configurable timeouts. | ✅ `TimeLimiterRegistry`; `WebhookDispatcher` HTTP connect/request timeouts |
| FR-30.2 | Circuit breakers protect dependent services. | ✅ `ResilienceConfig.circuitBreakerRegistry` |
| FR-30.3 | Retry policies are configurable per integration. | ✅ `ResilienceConfig.retryRegistry` |
| FR-30.4 | Bulkheads isolate downstream failures. | ✅ `ResilienceConfig.bulkheadRegistry`; applied in `WebhookDispatcher` |
