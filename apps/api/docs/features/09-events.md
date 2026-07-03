# Feature 9 — Events (Transactional Outbox)

**Package:** `com.bank.feature.events` · **Internal** (relay + consumer)

## Purpose
Fan out domain side-effects (notifications, projections, analytics) reliably,
without the dual-write problem. Money is **never** moved by events — only
side-effects.

## Layout
```
events/
├── web/        TransferNotificationConsumer (@RabbitListener)
├── domain/     EventPublisher + OutboxWriter, OutboxRelay
└── persistence/ OutboxEvent, OutboxStatus, OutboxEventRepository, ProcessedEvent, ProcessedEventRepository
```

## The flow
1. **Write** — `OutboxWriter.write(type, aggregateId, payload)` saves an
   `OutboxEvent(status=NEW)`. It uses `MANDATORY` propagation, so it runs inside
   the caller's transaction (e.g. a transfer). The event becomes durable **exactly
   when** the business change commits.
2. **Relay** — `OutboxRelay.publishBatch` (`@Scheduled`, 1s) reads up to 100 `NEW`
   rows, publishes each to the `bank.events` topic exchange with the event id as
   the message id, and flips them `PUBLISHED`.
3. **Consume** — `TransferNotificationConsumer` (`@RabbitListener`) dedups on the
   event id via `processed_events` (insert-first), then handles the message. This
   makes at-least-once delivery behave effectively-once.

## Why
The classic bug is "commit the DB row, then publish to the broker" — crash in
between and the event is lost (or published then the DB rolls back, emitting a
phantom). Putting the event in the same ACID transaction as the change eliminates
both failure modes.

## Interface seam
`EventPublisher` is one of the two genuine interfaces in the codebase: the
production `OutboxWriter` vs. an in-memory test alternate — callers
(`DefaultTransferService`) depend only on the port.

## Topology (`config/RabbitTopologyConfig`)
- Exchange: `bank.events` (topic) · DLX: `bank.events.dlx`
- Queue: `notifications.transfer-completed` bound on `transfer.completed`
- Poison messages dead-letter to the DLX.

## Code pointers
- Atomic write: `OutboxWriter.write`
- Relay: `OutboxRelay.publishBatch`
- Idempotent consumer: `TransferNotificationConsumer.onTransferCompleted`

## Related requirements
FR-8.*, NFR-2.3, NFR-2.4, NFR-2.9
