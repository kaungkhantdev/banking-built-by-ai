package com.bank.feature.notifications.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A configurable notification template (FR-13.5), keyed by a stable {@code code}
 * (e.g. {@code transfer_completed}). {@code subject}/{@code body} may contain
 * {@code {{var}}} placeholders resolved from the event payload at send time.
 */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    protected NotificationTemplate() {}

    public NotificationTemplate(String code, String subject, String body) {
        this.code = code;
        this.subject = subject;
        this.body = body;
    }

    public String getCode() { return code; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}
