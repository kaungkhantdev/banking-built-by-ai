-- V10: password history, reset tokens, and login attempt tracking (Feature 14)
CREATE TABLE password_history (
    id            UUID PRIMARY KEY,
    user_id       UUID NOT NULL REFERENCES users(id),
    password_hash TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_pwd_hist_user ON password_history (user_id, created_at DESC);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE login_attempts (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users(id),
    success      BOOLEAN NOT NULL,
    ip_address   TEXT,
    attempted_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_login_attempts_user ON login_attempts (user_id, attempted_at DESC);

-- track account lockout per user
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_attempt_count INT NOT NULL DEFAULT 0;
