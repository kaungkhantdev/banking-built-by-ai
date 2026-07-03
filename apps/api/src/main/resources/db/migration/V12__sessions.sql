-- V12: device records for session visibility (Feature 16)
CREATE TABLE device_records (
    id                 UUID PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users(id),
    session_id         UUID NOT NULL,
    user_agent         TEXT,
    ip_address         TEXT,
    device_fingerprint TEXT,
    logged_in_at       TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_device_records_user ON device_records (user_id, logged_in_at DESC);
CREATE INDEX ix_device_records_session ON device_records (session_id);
