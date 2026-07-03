# Feature 26 — Webhooks

**Package:** `com.bank.feature.webhooks` · **Endpoints:** `/v1/webhooks/*`

## Purpose
Allow third-party systems to register HTTP endpoints that receive signed event
payloads, with automatic retry on delivery failure and a delivery history log.

## Layout
```
webhooks/
├── web/        WebhookController, dto/ (WebhookRegistrationRequest, WebhookView)
├── domain/     WebhookService + DefaultWebhookService, WebhookDispatcher, PayloadSigner
└── persistence/ WebhookEndpoint, WebhookEndpointRepository, WebhookDelivery, WebhookDeliveryRepository
```

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/webhooks` | `webhook:manage` | Register an endpoint |
| GET | `/v1/webhooks` | `webhook:manage` | List registered endpoints |
| DELETE | `/v1/webhooks/{id}` | `webhook:manage` | Deregister an endpoint |
| GET | `/v1/webhooks/{id}/deliveries` | `webhook:manage` | View delivery history |

## How it works
1. Domain events flow through the Outbox relay (Feature 9) into an internal consumer.
2. `WebhookDispatcher` looks up registered endpoints subscribed to the event type and
   POSTs the payload to each.
3. Each payload is signed with HMAC-SHA256 using the endpoint's secret; the signature
   is included in the `X-Webhook-Signature` header so the receiver can verify it.
4. Each dispatch attempt is written to `WebhookDelivery` (status, response code, latency).
5. On non-2xx response, the dispatcher retries with exponential backoff up to a
   configurable max-attempts before marking the delivery `FAILED`.

## Key rules
- Webhook secrets are stored encrypted at rest and are never returned after registration.
- Delivery history is retained for a configurable retention period (e.g., 30 days).
- Retries are asynchronous and do not block the originating event consumer.

## Why
HMAC signing lets receivers reject forged payloads without a round-trip to the API.
Exponential backoff prevents a slow receiver from creating a thundering-herd retry storm.

## Related requirements
FR-26.*, FR-8.*
