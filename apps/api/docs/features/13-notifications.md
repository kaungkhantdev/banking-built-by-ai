# Feature 13 — Notifications

**Package:** `com.bank.feature.notifications` · **Endpoints:** none (event-driven)

## Purpose
Deliver transactional notifications (email, push) to users in response to domain
events: successful transfers, KYC status changes, and password changes.

## Layout
```
notifications/
├── consumer/   TransferNotificationConsumer, KycNotificationConsumer, SecurityNotificationConsumer
├── domain/     NotificationService + DefaultNotificationService, NotificationTemplate
└── persistence/ NotificationLog, NotificationLogRepository
```

## How it works
1. Domain events published to RabbitMQ by the Outbox relay (Feature 9) are consumed here.
2. The consumer resolves the appropriate template, renders it, and delegates to the
   transport layer (e.g., `EmailSender`).
3. Every dispatch attempt is written to `NotificationLog` for observability.
4. On delivery failure the consumer does **not** ack the message; RabbitMQ retries
   with dead-lettering up to a configured max-attempts before parking in the DLQ.

## Key rules
- Consumers are idempotent: the `NotificationLog` deduplicates on `(event_id, channel)`.
- Templates are stored externally (e.g., classpath or a config store) so content can
  change without a redeploy.
- Notification delivery must never block or roll back the originating business transaction.

## Why
Decoupling notification delivery from the business transaction via the outbox ensures
a failed email never causes a transfer to roll back, while the retry/DLQ pattern
guarantees at-least-once delivery.

## Related requirements
FR-13.*, FR-8.4
