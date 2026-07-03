package com.bank.feature.beneficiaries.web.dto;

import com.bank.feature.beneficiaries.persistence.Beneficiary;

import java.time.Instant;
import java.util.UUID;

public record BeneficiaryView(
        UUID id,
        UUID ownerUserId,
        String alias,
        UUID destinationWalletId,
        Instant createdAt
) {
    public static BeneficiaryView of(Beneficiary b) {
        return new BeneficiaryView(b.getId(), b.getOwnerUserId(), b.getAlias(),
                b.getDestinationWalletId(), b.getCreatedAt());
    }
}
