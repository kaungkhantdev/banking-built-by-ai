-- V5: transactional outbox + consumer dedup ledger.

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    type         TEXT NOT NULL,
    aggregate_id TEXT NOT NULL,
    payload      TEXT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('NEW','PUBLISHED')),
    created_at   TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX ix_outbox_status_created ON outbox_events (status, created_at);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
