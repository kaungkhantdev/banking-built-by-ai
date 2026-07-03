package com.bank.feature.fraud.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fraud_rule_configs")
public class FraudRuleConfig extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String ruleName;

    @Column(precision = 19, scale = 4)
    private BigDecimal thresholdValue;

    @Column
    private Integer windowMinutes;

    @Column
    private Integer maxCount;

    @Column(nullable = false)
    private int scoreIncrement;

    @Column(nullable = false)
    private int blockScore;

    @Column(nullable = false)
    private boolean active;

    protected FraudRuleConfig() {}

    public String getRuleName() { return ruleName; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public Integer getWindowMinutes() { return windowMinutes; }
    public Integer getMaxCount() { return maxCount; }
    public int getScoreIncrement() { return scoreIncrement; }
    public int getBlockScore() { return blockScore; }
    public boolean isActive() { return active; }
}
