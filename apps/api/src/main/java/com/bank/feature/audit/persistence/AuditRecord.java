package com.bank.feature.audit.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit record: who did what, when, what changed, and the trace id
 * that ties it to logs and the client-visible error envelope. Independent of
 * application logs (which rotate and are mutable).
 */
@Entity
@Table(name = "audit_records", indexes = {
        @Index(name = "ix_audit_actor", columnList = "actor"),
        @Index(name = "ix_audit_action", columnList = "action")
})
public class AuditRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "text")
    private String beforeJson;

    @Column(columnDefinition = "text")
    private String afterJson;

    @Column(nullable = false)
    private String traceId;

    @Column(nullable = false)
    private Instant at;

    protected AuditRecord() {
    }

    public AuditRecord(String actor, String action, String beforeJson,
                       String afterJson, String traceId) {
        this.actor = actor;
        this.action = action;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.traceId = traceId;
        this.at = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public Instant getAt() {
        return at;
    }
}
