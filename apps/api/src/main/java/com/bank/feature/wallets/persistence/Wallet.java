package com.bank.feature.wallets.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * A currency-scoped balance container under an account. The balance is NOT
 * stored here — it is derived by summing ledger entries (see ledger feature).
 * One wallet per (account, currency).
 */
@Entity
@Table(name = "wallets",
        uniqueConstraints = @UniqueConstraint(name = "uq_wallet_account_ccy",
                columnNames = {"accountId", "currency"}))
public class Wallet extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false, length = 3)
    private String currency;     // ISO-4217

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    protected Wallet() {
    }

    public Wallet(UUID accountId, String currency) {
        this.accountId = accountId;
        this.currency = currency;
        this.status = WalletStatus.ACTIVE;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }
}
