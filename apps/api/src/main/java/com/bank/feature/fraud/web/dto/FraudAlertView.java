package com.bank.feature.fraud.web.dto;

import com.bank.feature.fraud.persistence.FraudAlert;

import java.time.Instant;
import java.util.UUID;

public record FraudAlertView(
        UUID id,
        UUID transferId,
        UUID userId,
        int riskScore,
        String status,
        String ruleDetails,
        Instant createdAt) {

    public static FraudAlertView of(FraudAlert a) {
        return new FraudAlertView(a.getId(), a.getTransferId(), a.getUserId(),
                a.getRiskScore(), a.getStatus(), a.getRuleDetails(), a.getCreatedAt());
    }
}
