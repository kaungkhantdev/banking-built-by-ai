-- V23: file metadata for KYC docs, statements and reports (Feature 27)
CREATE TABLE file_metas (
    id            UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    file_name     TEXT NOT NULL,
    content_type  TEXT NOT NULL,
    size_bytes    BIGINT,
    storage_key   TEXT NOT NULL UNIQUE,
    status        TEXT NOT NULL CHECK (status IN ('PENDING','READY','QUARANTINED','DELETED')) DEFAULT 'PENDING',
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_file_metas_owner ON file_metas (owner_user_id, created_at DESC);
