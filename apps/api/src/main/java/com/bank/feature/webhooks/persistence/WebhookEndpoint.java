package com.bank.feature.webhooks.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String secretEncrypted;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventTypes;

    @Column(nullable = false)
    private boolean active;

    protected WebhookEndpoint() {}

    public WebhookEndpoint(UUID ownerUserId, String url, String secretEncrypted, String eventTypes) {
        this.ownerUserId = ownerUserId;
        this.url = url;
        this.secretEncrypted = secretEncrypted;
        this.eventTypes = eventTypes;
        this.active = true;
    }

    public UUID getOwnerUserId() { return ownerUserId; }
    public String getUrl() { return url; }
    public String getSecretEncrypted() { return secretEncrypted; }
    public String getEventTypes() { return eventTypes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
