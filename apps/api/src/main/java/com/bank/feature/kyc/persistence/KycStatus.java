package com.bank.feature.kyc.persistence;

/** KYC case lifecycle. Guarded transitions only; money-out requires VERIFIED. */
public enum KycStatus {
    CREATED, DOCS_SUBMITTED, UNDER_REVIEW, VERIFIED, REJECTED
}
