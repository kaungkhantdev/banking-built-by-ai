-- V8: customer profiles (canonical identity, separate from auth User)
CREATE TABLE customers (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id),
    full_name      TEXT NOT NULL,
    phone          TEXT,
    date_of_birth  DATE,
    status         TEXT NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')) DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ix_customers_user ON customers (user_id);
CREATE INDEX ix_customers_name ON customers (lower(full_name));
CREATE INDEX ix_customers_phone ON customers (phone);
