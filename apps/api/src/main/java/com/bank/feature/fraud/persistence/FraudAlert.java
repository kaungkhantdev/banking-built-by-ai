package com.bank.feature.fraud.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert extends BaseAuditEntity {

    @Column
    private UUID transferId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int riskScore;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String ruleDetails;

    protected FraudAlert() {}

    public FraudAlert(UUID transferId, UUID userId, int riskScore, String status, String ruleDetails) {
        this.transferId = transferId;
        this.userId = userId;
        this.riskScore = riskScore;
        this.status = status;
        this.ruleDetails = ruleDetails;
    }

    public UUID getTransferId() { return transferId; }
    public UUID getUserId() { return userId; }
    public int getRiskScore() { return riskScore; }
    public String getStatus() { return status; }
    public String getRuleDetails() { return ruleDetails; }
    public void setStatus(String status) { this.status = status; }
}
