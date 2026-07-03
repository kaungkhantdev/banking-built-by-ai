-- V17: fraud rule configuration and alert records (Feature 22)
CREATE TABLE fraud_rule_configs (
    id              UUID PRIMARY KEY,
    rule_name       TEXT NOT NULL UNIQUE,
    threshold_value NUMERIC(19,4),
    window_minutes  INT,
    max_count       INT,
    score_increment INT NOT NULL DEFAULT 10,
    block_score     INT NOT NULL DEFAULT 80,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE fraud_alerts (
    id           UUID PRIMARY KEY,
    transfer_id  UUID,
    user_id      UUID NOT NULL,
    risk_score   INT NOT NULL,
    status       TEXT NOT NULL CHECK (status IN ('BLOCKED','PENDING_REVIEW','APPROVED','FALSE_POSITIVE')) DEFAULT 'BLOCKED',
    rule_details TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_fraud_alerts_user ON fraud_alerts (user_id, created_at DESC);

-- default fraud rules
INSERT INTO fraud_rule_configs (id, rule_name, threshold_value, score_increment, block_score, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'HIGH_VALUE', 5000.0000, 50, 80, NOW(), NOW());

INSERT INTO fraud_rule_configs (id, rule_name, window_minutes, max_count, score_increment, block_score, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'VELOCITY', 60, 10, 40, 80, NOW(), NOW());
