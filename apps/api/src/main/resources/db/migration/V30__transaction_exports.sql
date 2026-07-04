-- V30: async transaction CSV export jobs (FR-12.4, scalable path).
CREATE TABLE transaction_exports (
    id            UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    wallet_id     UUID NOT NULL,
    direction     TEXT,
    currency      VARCHAR(3),
    from_ts       TIMESTAMPTZ,
    to_ts         TIMESTAMPTZ,
    status        TEXT NOT NULL DEFAULT 'QUEUED',
    file_id       UUID,
    row_count     INT,
    error         TEXT,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_transaction_exports_owner ON transaction_exports (owner_user_id, created_at DESC);
