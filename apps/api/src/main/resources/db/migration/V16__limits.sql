-- V16: per-tier transfer limits and usage tracking (Feature 20)
CREATE TABLE limit_configs (
    id            UUID PRIMARY KEY,
    customer_tier TEXT NOT NULL DEFAULT 'STANDARD',
    period        TEXT NOT NULL CHECK (period IN ('DAILY','MONTHLY')),
    max_amount    NUMERIC(19,4) NOT NULL,
    currency      CHAR(3) NOT NULL DEFAULT 'USD',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_limit_config UNIQUE (customer_tier, period, currency)
);

CREATE TABLE limit_usages (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    period      TEXT NOT NULL CHECK (period IN ('DAILY','MONTHLY')),
    period_key  TEXT NOT NULL,
    used_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency    CHAR(3) NOT NULL DEFAULT 'USD',
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_limit_usage UNIQUE (user_id, period, period_key, currency)
);

-- default STANDARD-tier limits (10k/day, 50k/month in USD)
INSERT INTO limit_configs (id, customer_tier, period, max_amount, currency, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'STANDARD', 'DAILY',   10000.0000, 'USD', NOW(), NOW()),
  (gen_random_uuid(), 'STANDARD', 'MONTHLY', 50000.0000, 'USD', NOW(), NOW());
