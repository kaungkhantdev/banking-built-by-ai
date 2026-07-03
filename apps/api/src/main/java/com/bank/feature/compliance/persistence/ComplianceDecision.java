package com.bank.feature.compliance.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_decisions")
public class ComplianceDecision {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private String ruleVersion;

    @Column(nullable = false)
    private String decision; // PASS, BLOCK, EDD

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private Instant createdAt;

    protected ComplianceDecision() {}

    public ComplianceDecision(String entityType, UUID entityId, String ruleVersion,
                               String decision, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.ruleVersion = ruleVersion;
        this.decision = decision;
        this.details = details;
        this.createdAt = Instant.now();
    }

    public String getDecision() { return decision; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
}
