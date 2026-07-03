-- V2: the money core — append-only ledger, idempotency keys.
-- ledger_entries is append-only at the privilege level: grant no UPDATE/DELETE.

CREATE TABLE idempotency_keys (
    id             UUID PRIMARY KEY,
    idem_key       TEXT NOT NULL,
    transaction_id UUID NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idem_key UNIQUE (idem_key)   -- DB is the source of truth, not app memory
);

CREATE TABLE ledger_entries (
    id             UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    wallet_id      UUID NOT NULL,
    direction      TEXT NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount         NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency       CHAR(3) NOT NULL,
    posted_at      TIMESTAMPTZ NOT NULL,
    memo           TEXT
);
CREATE INDEX ix_ledger_wallet ON ledger_entries (wallet_id);
CREATE INDEX ix_ledger_tx     ON ledger_entries (transaction_id);
