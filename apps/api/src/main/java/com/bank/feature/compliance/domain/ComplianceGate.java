package com.bank.feature.compliance.domain;

import com.bank.feature.compliance.persistence.ComplianceDecision;
import com.bank.feature.compliance.persistence.ComplianceDecisionRepository;
import com.bank.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Screens customers and transfers against sanctions/AML/PEP rules. Must run inside caller's transaction. */
@Service
public class ComplianceGate {

    private static final String RULE_VERSION = "1.0";

    private final ComplianceDecisionRepository decisions;
    /** Stub sanctions and PEP watchlists (config-driven; a real vendor API in prod). */
    private final Set<String> sanctionsList;
    private final Set<String> pepList;

    public ComplianceGate(ComplianceDecisionRepository decisions,
                          @Value("${compliance.sanctions-list:}") List<String> sanctionsList,
                          @Value("${compliance.pep-list:}") List<String> pepList) {
        this.decisions = decisions;
        this.sanctionsList = normalize(sanctionsList);
        this.pepList = normalize(pepList);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void screenCustomer(UUID customerId, String fullName) {
        // FR-23.1: sanctions screening — a hit blocks onboarding.
        String sanctions = screenSanctions(fullName);
        decisions.save(new ComplianceDecision("CUSTOMER", customerId, RULE_VERSION, sanctions, null));
        if ("BLOCK".equals(sanctions)) {
            throw new ApiException("COMPLIANCE_BLOCK",
                    "Customer failed sanctions screening", 422);
        }

        // FR-23.2: PEP screening — a match doesn't block but flags Enhanced Due Diligence.
        boolean pep = screenPep(fullName);
        decisions.save(new ComplianceDecision("PEP", customerId, RULE_VERSION,
                pep ? "EDD" : "PASS",
                pep ? "Politically Exposed Person match" : null));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void screenTransfer(UUID transactionId, UUID fromUserId, UUID toUserId) {
        // AML inspection stub — always PASS; a real engine checks structuring/velocity.
        decisions.save(new ComplianceDecision("TRANSFER", transactionId, RULE_VERSION, "PASS", null));
    }

    /** BLOCK if the name is on the sanctions watchlist, else PASS. */
    private String screenSanctions(String name) {
        return name != null && sanctionsList.contains(name.trim().toLowerCase()) ? "BLOCK" : "PASS";
    }

    /** True if the name matches the Politically Exposed Persons watchlist. */
    private boolean screenPep(String name) {
        return name != null && pepList.contains(name.trim().toLowerCase());
    }

    private static Set<String> normalize(List<String> values) {
        if (values == null) return Set.of();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .collect(Collectors.toSet());
    }
}
