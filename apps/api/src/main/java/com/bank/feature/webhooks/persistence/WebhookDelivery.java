package com.bank.feature.webhooks.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A single delivery attempt-set for one event to one endpoint (FR-26.4). Retried
 * with backoff until DELIVERED or the attempt budget is exhausted (FR-26.3).
 */
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID endpointId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String status;   // PENDING, DELIVERED, FAILED

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    @Column
    private Integer responseCode;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    protected WebhookDelivery() {}

    public WebhookDelivery(UUID endpointId, String eventType, String payload) {
        this.endpointId = endpointId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.attempts = 0;
        this.maxAttempts = 5;
        this.nextAttemptAt = Instant.now();
    }

    public UUID getEndpointId() { return endpointId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Integer getResponseCode() { return responseCode; }
    public String getLastError() { return lastError; }

    public void recordSuccess(int responseCode) {
        this.attempts++;
        this.responseCode = responseCode;
        this.status = "DELIVERED";
        this.lastError = null;
    }

    /** Record a failed attempt and schedule the next with exponential backoff. */
    public void recordFailure(Integer responseCode, String error) {
        this.attempts++;
        this.responseCode = responseCode;
        this.lastError = error;
        if (this.attempts >= this.maxAttempts) {
            this.status = "FAILED";
        } else {
            long backoffSeconds = (long) Math.pow(2, this.attempts);   // 2,4,8,16s...
            this.nextAttemptAt = Instant.now().plusSeconds(backoffSeconds);
        }
    }
}
