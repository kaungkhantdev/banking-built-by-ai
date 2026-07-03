-- V13: account statement records (Feature 17)
CREATE TABLE statement_records (
    id           UUID PRIMARY KEY,
    account_id   UUID NOT NULL,
    period_year  INT NOT NULL,
    period_month INT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('PENDING','READY','FAILED')) DEFAULT 'PENDING',
    file_key     TEXT,
    digest       TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_statement_account_period UNIQUE (account_id, period_year, period_month)
);
CREATE INDEX ix_statement_account ON statement_records (account_id, period_year DESC, period_month DESC);
