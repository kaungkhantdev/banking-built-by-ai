-- V11: TOTP MFA credentials and single-use recovery codes (Feature 15)
CREATE TABLE mfa_credentials (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL UNIQUE REFERENCES users(id),
    secret_encrypted TEXT NOT NULL,
    confirmed        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE recovery_codes (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    code_hash  TEXT NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_recovery_codes_user ON recovery_codes (user_id);
