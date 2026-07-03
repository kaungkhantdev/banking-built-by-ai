-- V18: compliance screening decisions (Feature 23)
CREATE TABLE compliance_decisions (
    id           UUID PRIMARY KEY,
    entity_type  TEXT NOT NULL,
    entity_id    UUID NOT NULL,
    rule_version TEXT NOT NULL DEFAULT '1.0',
    decision     TEXT NOT NULL CHECK (decision IN ('PASS','BLOCK','EDD')),
    details      TEXT,
    created_at   TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_compliance_entity ON compliance_decisions (entity_id, entity_type, created_at DESC);
