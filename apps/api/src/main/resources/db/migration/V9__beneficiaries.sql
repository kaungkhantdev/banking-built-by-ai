-- V9: saved transfer destinations (beneficiaries)
CREATE TABLE beneficiaries (
    id                    UUID PRIMARY KEY,
    owner_user_id         UUID NOT NULL,
    alias                 TEXT NOT NULL,
    destination_wallet_id UUID NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_beneficiary_owner_wallet UNIQUE (owner_user_id, destination_wallet_id)
);
CREATE INDEX ix_beneficiaries_owner ON beneficiaries (owner_user_id);
