-- V1: identity, RBAC, accounts, wallets
-- Core reference + customer-relationship tables.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    session_id  UUID NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    previous_id UUID,
    status      TEXT NOT NULL CHECK (status IN ('ACTIVE','ROTATED','REVOKED')),
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX ix_refresh_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_session ON refresh_tokens (session_id);

CREATE TABLE roles (
    id   UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE permissions (
    id   UUID PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    id      UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id)
);
CREATE INDEX ix_user_roles_user ON user_roles (user_id);

CREATE TABLE accounts (
    id            UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    status        TEXT NOT NULL CHECK (status IN ('PENDING','ACTIVE','FROZEN','CLOSED')),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE wallets (
    id         UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    currency   CHAR(3) NOT NULL,
    status     TEXT NOT NULL CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_wallet_account_ccy UNIQUE (account_id, currency)
);
