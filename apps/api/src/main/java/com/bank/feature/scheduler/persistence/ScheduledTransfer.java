package com.bank.feature.scheduler.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_transfers")
public class ScheduledTransfer extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private UUID fromWalletId;

    @Column(nullable = false)
    private UUID toWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column
    private String memo;

    @Column
    private String recurrenceRule;

    @Column(nullable = false)
    private Instant nextRunAt;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column
    private Instant lastRunAt;

    protected ScheduledTransfer() {}

    public ScheduledTransfer(UUID ownerUserId, UUID fromWalletId, UUID toWalletId,
                              BigDecimal amount, String memo, String recurrenceRule, Instant nextRunAt) {
        this.ownerUserId = ownerUserId;
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.memo = memo;
        this.recurrenceRule = recurrenceRule;
        this.nextRunAt = nextRunAt;
        this.status = "ACTIVE";
        this.attemptCount = 0;
        this.maxAttempts = 3;
    }

    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getFromWalletId() { return fromWalletId; }
    public UUID getToWalletId() { return toWalletId; }
    public BigDecimal getAmount() { return amount; }
    public String getMemo() { return memo; }
    public String getRecurrenceRule() { return recurrenceRule; }
    public Instant getNextRunAt() { return nextRunAt; }
    public String getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLastError() { return lastError; }
    public Instant getLastRunAt() { return lastRunAt; }

    public void setStatus(String status) { this.status = status; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setLastRunAt(Instant lastRunAt) { this.lastRunAt = lastRunAt; }
    public void incrementAttemptCount() { this.attemptCount++; }
}
