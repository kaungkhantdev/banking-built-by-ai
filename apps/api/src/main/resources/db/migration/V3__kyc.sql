-- V3: KYC case state machine (one case per account).

CREATE TABLE kyc_cases (
    id            UUID PRIMARY KEY,
    account_id    UUID NOT NULL UNIQUE,
    status        TEXT NOT NULL CHECK (status IN
                  ('CREATED','DOCS_SUBMITTED','UNDER_REVIEW','VERIFIED','REJECTED')),
    vendor_ref    TEXT,
    reject_reason TEXT,
    updated_at    TIMESTAMPTZ NOT NULL
);
