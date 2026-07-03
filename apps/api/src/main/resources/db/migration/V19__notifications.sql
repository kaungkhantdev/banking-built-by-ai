-- V19: notification delivery log (Feature 13)
CREATE TABLE notification_log (
    id        UUID PRIMARY KEY,
    event_id  UUID NOT NULL,
    channel   TEXT NOT NULL,
    recipient TEXT NOT NULL,
    template  TEXT NOT NULL,
    status    TEXT NOT NULL CHECK (status IN ('SENT','FAILED')) DEFAULT 'SENT',
    sent_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_event_channel UNIQUE (event_id, channel)
);
