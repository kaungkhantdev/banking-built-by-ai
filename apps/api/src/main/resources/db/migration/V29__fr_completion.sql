-- V29: schema + seed to complete FR-10..FR-30 wiring.
-- (Login-lockout columns locked_until / failed_attempt_count already exist from V10.)

-- FR-13.5: configurable notification templates.
CREATE TABLE notification_templates (
    code    TEXT PRIMARY KEY,
    subject TEXT NOT NULL,
    body    TEXT NOT NULL
);
INSERT INTO notification_templates (code, subject, body) VALUES
    ('transfer_completed', 'Transfer completed',
     'Your transfer of {{amount}} {{currency}} completed (fee {{fee}}).'),
    ('kyc_status_changed', 'KYC status updated',
     'Your KYC status is now {{status}}.'),
    ('password_changed', 'Password changed',
     'Your account password was just changed. If this was not you, contact support immediately.')
ON CONFLICT (code) DO NOTHING;

-- FR-26.4: webhook delivery history (append-only attempt log).
-- V22 created an earlier, unused webhook_deliveries shape with no JPA entity;
-- replace it with the schema the delivery worker actually uses.
DROP TABLE IF EXISTS webhook_deliveries CASCADE;
CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY,
    endpoint_id     UUID NOT NULL,
    event_type      TEXT NOT NULL,
    payload         TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    response_code   INT,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_webhook_deliveries_due ON webhook_deliveries (status, next_attempt_at);
CREATE INDEX ix_webhook_deliveries_endpoint ON webhook_deliveries (endpoint_id, created_at DESC);

-- FR-27.3/27.4: encrypted file content at rest + upload-URL expiry.
ALTER TABLE file_metas ADD COLUMN content_encrypted TEXT;
ALTER TABLE file_metas ADD COLUMN upload_expires_at TIMESTAMPTZ;

-- FR-22: fraud alert management permissions, granted to OPERATOR.
INSERT INTO permissions (id, name) VALUES
    (gen_random_uuid(), 'fraud:read'),
    (gen_random_uuid(), 'fraud:manage')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'OPERATOR' AND p.name IN ('fraud:read', 'fraud:manage')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR' AND p.name IN ('fraud:read')
ON CONFLICT DO NOTHING;

-- FR-19.3: system account that owns the bank's fee-collection wallets.
INSERT INTO users (id, email, password_hash, enabled, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-0000000f0ee1'::uuid, 'fees@system.local',
        'x', FALSE, NOW(), NOW(), 0)
ON CONFLICT (email) DO NOTHING;

INSERT INTO accounts (id, owner_user_id, status, created_at, updated_at, version)
VALUES ('00000000-0000-0000-0000-0000000f0ee5'::uuid,
        '00000000-0000-0000-0000-0000000f0ee1'::uuid, 'ACTIVE', NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;
