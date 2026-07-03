package com.bank.feature.kyc.domain;

import java.util.UUID;

/**
 * Port to an external KYC verification vendor. This is one of the two interfaces
 * with a genuine second implementation — the HTTP vendor in production and a
 * deterministic stub in tests/sandbox.
 */
public interface KycVendorClient {

    VendorResult verify(UUID accountId);

    record VendorResult(boolean approved, String reference, String reason) {
    }
}
