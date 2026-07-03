package com.bank.feature.ledger.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One leg of a double-entry posting. <b>Append-only</b>: never updated or
 * deleted. The {@code amount} is always positive; the sign is carried by
 * {@link Direction}. Entries sharing a {@code transactionId} are the balanced
 * legs of a single money movement.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "ix_ledger_wallet", columnList = "walletId"),
        @Index(name = "ix_ledger_tx", columnList = "transactionId")
})
public class LedgerEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant postedAt;

    @Column
    private String memo;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID transactionId, UUID walletId, Direction direction,
                       BigDecimal amount, String currency, Instant postedAt, String memo) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.postedAt = postedAt;
        this.memo = memo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public String getMemo() {
        return memo;
    }
}
