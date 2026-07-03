package com.bank.feature.kyc.domain;

import com.bank.feature.kyc.persistence.KycStatus;
import com.bank.feature.kyc.web.dto.KycCaseView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** Port: KYC onboarding and case progression. */
public interface KycService {

    /** Create a case for an account (idempotent: returns existing if present). */
    UUID openCase(UUID accountId);

    /** Record a submitted document and kick off async vendor verification. */
    void submitDocument(UUID accountId, byte[] documentBytes, String contentType);

    KycStatus statusOf(UUID accountId);

    /** Paginated list of KYC cases, optionally filtered by status. */
    Page<KycCaseView> list(KycStatus status, Pageable pageable);
}
