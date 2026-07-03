package com.bank.feature.events.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox row. Written inside the same DB transaction as the
 * business change, so the event becomes durable exactly when the change does —
 * eliminating the dual-write problem. The {@code id} doubles as the dedup key
 * for idempotent consumers.
 */
@Entity
@Table(name = "outbox_events",
        indexes = @Index(name = "ix_outbox_status_created", columnList = "status, createdAt"))
public class OutboxEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String type;          // e.g. "transfer.completed"

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;       // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(String type, String aggregateId, String payload) {
        this.type = type;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxStatus.NEW;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }
}
