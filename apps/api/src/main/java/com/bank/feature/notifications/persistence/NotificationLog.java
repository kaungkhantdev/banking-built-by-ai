package com.bank.feature.notifications.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/** Append-only record of every notification dispatch attempt. */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String template;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant sentAt;

    protected NotificationLog() {}

    public NotificationLog(UUID eventId, String channel, String recipient,
                            String template, String status) {
        this.eventId = eventId;
        this.channel = channel;
        this.recipient = recipient;
        this.template = template;
        this.status = status;
        this.sentAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getTemplate() { return template; }
    public String getStatus() { return status; }
    public Instant getSentAt() { return sentAt; }
}
