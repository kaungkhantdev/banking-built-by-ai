-- V15: fee configuration and waiver records (Feature 19)
CREATE TABLE fee_configs (
    id            UUID PRIMARY KEY,
    transfer_type TEXT NOT NULL DEFAULT 'STANDARD',
    customer_tier TEXT NOT NULL DEFAULT 'STANDARD',
    fee_type      TEXT NOT NULL CHECK (fee_type IN ('FLAT','PERCENTAGE')),
    fee_value     NUMERIC(19,4) NOT NULL,
    min_fee       NUMERIC(19,4),
    max_fee       NUMERIC(19,4),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE waiver_records (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL,
    fee_config_id UUID NOT NULL REFERENCES fee_configs(id),
    reason        TEXT,
    valid_from    TIMESTAMPTZ NOT NULL,
    valid_until   TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_waiver_user ON waiver_records (user_id, valid_from, valid_until);
