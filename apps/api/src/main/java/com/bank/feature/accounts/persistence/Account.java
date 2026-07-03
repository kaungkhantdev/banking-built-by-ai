package com.bank.feature.accounts.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** A customer's banking relationship. Wallets hang off an account. */
@Entity
@Table(name = "accounts")
public class Account extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    protected Account() {
    }

    public Account(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
        this.status = AccountStatus.PENDING;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
