package com.bank.feature.fees.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_configs")
public class FeeConfig extends BaseAuditEntity {

    @Column(nullable = false)
    private String transferType;

    @Column(nullable = false)
    private String customerTier;

    @Column(nullable = false)
    private String feeType; // FLAT or PERCENTAGE

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal feeValue;

    @Column(precision = 19, scale = 4)
    private BigDecimal minFee;

    @Column(precision = 19, scale = 4)
    private BigDecimal maxFee;

    @Column(nullable = false)
    private boolean active;

    protected FeeConfig() {}

    public String getTransferType() { return transferType; }
    public String getCustomerTier() { return customerTier; }
    public String getFeeType() { return feeType; }
    public BigDecimal getFeeValue() { return feeValue; }
    public BigDecimal getMinFee() { return minFee; }
    public BigDecimal getMaxFee() { return maxFee; }
    public boolean isActive() { return active; }
}
