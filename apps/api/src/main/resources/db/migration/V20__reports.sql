-- V20: report generation records (Feature 24)
CREATE TABLE report_records (
    id           UUID PRIMARY KEY,
    report_type  TEXT NOT NULL DEFAULT 'TRANSACTION_SUMMARY',
    from_date    DATE NOT NULL,
    to_date      DATE NOT NULL,
    format       TEXT NOT NULL CHECK (format IN ('CSV','XLSX')) DEFAULT 'CSV',
    status       TEXT NOT NULL CHECK (status IN ('PENDING','READY','FAILED')) DEFAULT 'PENDING',
    file_key     TEXT,
    filters      TEXT,
    requested_by UUID NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT NOT NULL DEFAULT 0
);
