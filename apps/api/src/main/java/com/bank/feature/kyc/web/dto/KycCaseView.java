package com.bank.feature.kyc.web.dto;

import com.bank.feature.kyc.persistence.KycStatus;

import java.time.Instant;
import java.util.UUID;

public record KycCaseView(UUID id, UUID accountId, KycStatus status, String vendorRef, String rejectReason, Instant updatedAt) {
}
