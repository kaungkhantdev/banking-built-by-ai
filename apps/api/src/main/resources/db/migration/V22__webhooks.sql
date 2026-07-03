-- V22: webhook endpoint registry and delivery history (Feature 26)
CREATE TABLE webhook_endpoints (
    id               UUID PRIMARY KEY,
    owner_user_id    UUID NOT NULL,
    url              TEXT NOT NULL,
    secret_encrypted TEXT NOT NULL,
    event_types      TEXT NOT NULL,
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_webhook_endpoints_owner ON webhook_endpoints (owner_user_id);

CREATE TABLE webhook_deliveries (
    id            UUID PRIMARY KEY,
    endpoint_id   UUID NOT NULL REFERENCES webhook_endpoints(id),
    event_type    TEXT NOT NULL,
    payload       TEXT NOT NULL,
    status        TEXT NOT NULL CHECK (status IN ('SUCCESS','FAILED','PENDING')) DEFAULT 'PENDING',
    http_status   INT,
    latency_ms    BIGINT,
    attempt_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_webhook_deliveries_ep ON webhook_deliveries (endpoint_id, created_at DESC);
