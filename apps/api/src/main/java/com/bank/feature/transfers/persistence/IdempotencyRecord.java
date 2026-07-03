package com.bank.feature.transfers.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * The DB-enforced idempotency record. The unique constraint on {@code idemKey}
 * is the source of truth (not application memory): a duplicate money POST hits
 * the constraint and replays the original {@code transactionId}.
 */
@Entity
@Table(name = "idempotency_keys",
        uniqueConstraints = @UniqueConstraint(name = "uq_idem_key", columnNames = "idemKey"))
public class IdempotencyRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String idemKey;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idemKey, UUID transactionId) {
        this.idemKey = idemKey;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
    }

    public static IdempotencyRecord of(String idemKey, UUID transactionId) {
        return new IdempotencyRecord(idemKey, transactionId);
    }

    public UUID getTransactionId() {
        return transactionId;
    }
}
