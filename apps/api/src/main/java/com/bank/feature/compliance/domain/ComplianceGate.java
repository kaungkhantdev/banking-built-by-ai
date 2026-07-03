package com.bank.feature.compliance.domain;

import com.bank.feature.compliance.persistence.ComplianceDecision;
import com.bank.feature.compliance.persistence.ComplianceDecisionRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Screens customers and transfers against sanctions/AML rules. Must run inside caller's transaction. */
@Service
public class ComplianceGate {

    private static final String RULE_VERSION = "1.0";

    private final ComplianceDecisionRepository decisions;

    public ComplianceGate(ComplianceDecisionRepository decisions) {
        this.decisions = decisions;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void screenCustomer(UUID customerId, String fullName) {
        // Stub: real implementation calls a sanctions/PEP API
        String decision = screenName(fullName);
        decisions.save(new ComplianceDecision("CUSTOMER", customerId, RULE_VERSION, decision, null));

        if ("BLOCK".equals(decision)) {
            throw new ApiException("COMPLIANCE_BLOCK",
                    "Customer failed sanctions screening", 422);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void screenTransfer(UUID transactionId, UUID fromUserId, UUID toUserId) {
        // Stub: always PASS — real implementation would check AML rules
        decisions.save(new ComplianceDecision("TRANSFER", transactionId, RULE_VERSION, "PASS", null));
    }

    /** Returns PASS in all stub cases; override with real sanctions API in production. */
    private String screenName(String name) {
        // In production, call an external sanctions API
        return "PASS";
    }
}
