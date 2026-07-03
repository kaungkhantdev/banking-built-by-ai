package com.bank.feature.beneficiaries.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private UUID destinationWalletId;

    protected Beneficiary() {}

    public Beneficiary(UUID ownerUserId, String alias, UUID destinationWalletId) {
        this.ownerUserId = ownerUserId;
        this.alias = alias;
        this.destinationWalletId = destinationWalletId;
    }

    public UUID getOwnerUserId() { return ownerUserId; }
    public String getAlias() { return alias; }
    public UUID getDestinationWalletId() { return destinationWalletId; }

    public void setAlias(String alias) { this.alias = alias; }
}
