-- V21: DB-backed scheduled / recurring transfers (Feature 25)
CREATE TABLE scheduled_transfers (
    id             UUID PRIMARY KEY,
    owner_user_id  UUID NOT NULL,
    from_wallet_id UUID NOT NULL,
    to_wallet_id   UUID NOT NULL,
    amount         NUMERIC(19,4) NOT NULL,
    memo           TEXT,
    recurrence_rule TEXT,
    next_run_at    TIMESTAMPTZ NOT NULL,
    status         TEXT NOT NULL CHECK (status IN ('ACTIVE','PAUSED','CANCELLED','FAILED')) DEFAULT 'ACTIVE',
    attempt_count  INT NOT NULL DEFAULT 0,
    max_attempts   INT NOT NULL DEFAULT 3,
    last_error     TEXT,
    last_run_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_scheduled_transfers_due ON scheduled_transfers (next_run_at)
    WHERE status = 'ACTIVE';
