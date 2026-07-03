-- V4: append-only audit trail.

CREATE TABLE audit_records (
    id          UUID PRIMARY KEY,
    actor       TEXT NOT NULL,
    action      TEXT NOT NULL,
    before_json TEXT,
    after_json  TEXT,
    trace_id    TEXT NOT NULL,
    at          TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_audit_actor  ON audit_records (actor);
CREATE INDEX ix_audit_action ON audit_records (action);
