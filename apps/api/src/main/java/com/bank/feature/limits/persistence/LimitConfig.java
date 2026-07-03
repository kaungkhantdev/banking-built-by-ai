package com.bank.feature.limits.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "limit_configs")
public class LimitConfig extends BaseAuditEntity {

    @Column(nullable = false)
    private String customerTier;

    @Column(nullable = false)
    private String period; // DAILY or MONTHLY

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean active;

    protected LimitConfig() {}

    public String getCustomerTier() { return customerTier; }
    public String getPeriod() { return period; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public String getCurrency() { return currency; }
    public boolean isActive() { return active; }
}
