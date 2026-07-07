package com.bank.feature.history.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An asynchronous transaction-CSV export job. The request returns 202 with this
 * row in {@code QUEUED}; a background worker generates the file into object
 * storage and flips it to {@code READY} (or {@code FAILED}). The filters are the
 * same as the live history query.
 */
@Entity
@Table(name = "transaction_exports")
public class TransactionExport extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private UUID walletId;

    @Column
    private String direction;

    @Column(length = 3)
    private String currency;

    @Column(name = "from_ts")
    private Instant fromTs;

    @Column(name = "to_ts")
    private Instant toTs;

    @Column(nullable = false)
    private String status;   // QUEUED, READY, FAILED

    /** Storage file id holding the generated CSV once READY. */
    @Column
    private UUID fileId;

    @Column
    private Integer rowCount;

    @Column(columnDefinition = "TEXT")
    private String error;

    protected TransactionExport() {}

    public TransactionExport(UUID ownerUserId, UUID walletId, String direction,
                             String currency, Instant fromTs, Instant toTs) {
        this.ownerUserId = ownerUserId;
        this.walletId = walletId;
        this.direction = direction;
        this.currency = currency;
        this.fromTs = fromTs;
        this.toTs = toTs;
        this.status = "QUEUED";
    }

    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getWalletId() { return walletId; }
    public String getDirection() { return direction; }
    public String getCurrency() { return currency; }
    public Instant getFromTs() { return fromTs; }
    public Instant getToTs() { return toTs; }
    public String getStatus() { return status; }
    public UUID getFileId() { return fileId; }
    public Integer getRowCount() { return rowCount; }
    public String getError() { return error; }

    public void markReady(UUID fileId, int rowCount) {
        this.fileId = fileId;
        this.rowCount = rowCount;
        this.status = "READY";
        this.error = null;
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.error = error;
    }
}
