-- V14: exchange rates and short-lived conversion quotes (Feature 18)
CREATE TABLE exchange_rates (
    id            UUID PRIMARY KEY,
    from_currency CHAR(3) NOT NULL,
    to_currency   CHAR(3) NOT NULL,
    rate          NUMERIC(19,8) NOT NULL,
    effective_at  TIMESTAMPTZ NOT NULL,
    source        TEXT,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_exchange_rates_pair ON exchange_rates (from_currency, to_currency, effective_at DESC);

CREATE TABLE exchange_quotes (
    id            UUID PRIMARY KEY,
    from_currency CHAR(3) NOT NULL,
    to_currency   CHAR(3) NOT NULL,
    from_amount   NUMERIC(19,4) NOT NULL,
    to_amount     NUMERIC(19,4) NOT NULL,
    rate          NUMERIC(19,8) NOT NULL,
    fee_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    rate_id       UUID NOT NULL REFERENCES exchange_rates(id),
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);
