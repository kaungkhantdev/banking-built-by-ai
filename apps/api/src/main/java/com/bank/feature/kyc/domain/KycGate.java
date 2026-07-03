package com.bank.feature.kyc.domain;

import com.bank.feature.kyc.persistence.KycCase;
import com.bank.feature.kyc.persistence.KycCaseRepository;
import com.bank.feature.kyc.persistence.KycStatus;
import com.bank.shared.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consulted by the money path before any money-out: an account may only send
 * funds when its KYC case is VERIFIED. Keeps the verification rule in one place.
 */
@Component
public class KycGate {

    private final KycCaseRepository cases;

    public KycGate(KycCaseRepository cases) {
        this.cases = cases;
    }

    public void assertMoneyOutAllowed(UUID accountId) {
        KycStatus status = cases.findByAccountId(accountId)
                .map(KycCase::getStatus)
                .orElse(KycStatus.CREATED);
        if (status != KycStatus.VERIFIED) {
            throw new ApiException("KYC_REQUIRED",
                    "Account KYC not verified (" + status + ")", 403);
        }
    }
}
