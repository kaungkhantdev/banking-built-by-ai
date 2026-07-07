package com.bank.feature.fraud.domain;

import com.bank.feature.events.domain.EventPublisher;
import com.bank.feature.fraud.persistence.FraudAlert;
import com.bank.feature.fraud.persistence.FraudAlertRepository;
import com.bank.feature.fraud.persistence.FraudRuleConfig;
import com.bank.feature.fraud.persistence.FraudRuleConfigRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Synchronous fraud assessment — must run inside the caller's transfer transaction. */
@Service
public class FraudEngine {

    private final FraudRuleConfigRepository rules;
    private final FraudAlertRepository alerts;
    private final EventPublisher events;

    public FraudEngine(FraudRuleConfigRepository rules, FraudAlertRepository alerts,
                       EventPublisher events) {
        this.rules = rules;
        this.alerts = alerts;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assess(UUID transactionId, UUID userId, BigDecimal amount) {
        List<FraudRuleConfig> activeRules = rules.findByActiveTrue();
        int score = 0;
        List<String> triggered = new ArrayList<>();

        for (FraudRuleConfig rule : activeRules) {
            switch (rule.getRuleName()) {
                case "HIGH_VALUE" -> {
                    if (rule.getThresholdValue() != null
                            && amount.compareTo(rule.getThresholdValue()) > 0) {
                        score += rule.getScoreIncrement();
                        triggered.add("HIGH_VALUE:" + amount);
                    }
                }
                case "VELOCITY" -> {
                    if (rule.getWindowMinutes() != null && rule.getMaxCount() != null) {
                        Instant since = Instant.now().minus(rule.getWindowMinutes(), ChronoUnit.MINUTES);
                        long count = alerts.countRecentByUserId(userId, since);
                        if (count >= rule.getMaxCount()) {
                            score += rule.getScoreIncrement();
                            triggered.add("VELOCITY:" + count + "/" + rule.getWindowMinutes() + "m");
                        }
                    }
                }
            }
        }

        if (score == 0 || activeRules.isEmpty()) return;

        int blockThreshold = activeRules.stream().mapToInt(FraudRuleConfig::getBlockScore).min().orElse(80);

        if (score >= blockThreshold) {
            // FR-22.4: hold the transfer — it cannot post until an operator approves.
            FraudAlert alert = alerts.save(new FraudAlert(transactionId, userId, score,
                    "BLOCKED", String.join(", ", triggered)));
            emit(alert, "fraud.alert.blocked");
            throw new ApiException("TRANSFER_HELD_FOR_REVIEW",
                    "Transfer held for manual approval: fraud risk score " + score, 422);
        }

        // Warn band: create alert but allow transfer
        FraudAlert alert = alerts.save(new FraudAlert(transactionId, userId, score,
                "PENDING_REVIEW", String.join(", ", triggered)));
        emit(alert, "fraud.alert.flagged");
    }

    /** FR-22.5: every fraud alert emits a domain event for audit/notification. */
    private void emit(FraudAlert alert, String type) {
        events.write(type, alert.getId(), Map.of(
                "transferId", String.valueOf(alert.getTransferId()),
                "userId", String.valueOf(alert.getUserId()),
                "riskScore", alert.getRiskScore(),
                "status", alert.getStatus(),
                "rules", String.valueOf(alert.getRuleDetails())));
    }
}
