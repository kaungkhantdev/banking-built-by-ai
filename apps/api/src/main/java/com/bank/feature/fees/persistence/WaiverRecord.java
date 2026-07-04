package com.bank.feature.fees.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A promotional fee waiver: while {@code now} is within [validFrom, validUntil]
 * the payer's transfer fee is waived. Append-only — mirrors {@code waiver_records}
 * (V15), which has no updated_at/version, so it does not extend BaseAuditEntity.
 */
@Entity
@Table(name = "waiver_records")
public class WaiverRecord {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID feeConfigId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validUntil;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected WaiverRecord() {}

    public WaiverRecord(UUID userId, UUID feeConfigId, String reason,
                        Instant validFrom, Instant validUntil) {
        this.userId = userId;
        this.feeConfigId = feeConfigId;
        this.reason = reason;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getFeeConfigId() { return feeConfigId; }
    public String getReason() { return reason; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
