package com.bank.feature.kyc.domain;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default vendor client. In a real deployment this performs an HTTP call to the
 * verification provider; here it approves deterministically so the platform runs
 * end-to-end without an external dependency. Swap via profile for the real one.
 */
@Component
@Profile("!realkyc")
public class StubKycVendorClient implements KycVendorClient {

    @Override
    public VendorResult verify(UUID accountId) {
        return new VendorResult(true, "stub-" + accountId, null);
    }
}
