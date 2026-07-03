package com.bank.feature.limits.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "limit_usages")
public class LimitUsage extends BaseAuditEntity {

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private String periodKey;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal usedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    protected LimitUsage() {}

    public LimitUsage(UUID userId, String period, String periodKey, String currency) {
        this.userId = userId;
        this.period = period;
        this.periodKey = periodKey;
        this.usedAmount = BigDecimal.ZERO;
        this.currency = currency;
    }

    public UUID getUserId() { return userId; }
    public String getPeriod() { return period; }
    public String getPeriodKey() { return periodKey; }
    public BigDecimal getUsedAmount() { return usedAmount; }
    public String getCurrency() { return currency; }

    public void addUsage(BigDecimal amount) {
        this.usedAmount = this.usedAmount.add(amount);
    }
}
