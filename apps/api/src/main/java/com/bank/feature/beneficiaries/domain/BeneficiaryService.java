package com.bank.feature.beneficiaries.domain;

import com.bank.feature.beneficiaries.web.dto.BeneficiaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BeneficiaryService {

    BeneficiaryView create(UUID ownerUserId, String alias, UUID destinationWalletId);

    Page<BeneficiaryView> list(UUID ownerUserId, Pageable pageable);

    BeneficiaryView update(UUID id, UUID ownerUserId, String alias);

    void delete(UUID id, UUID ownerUserId);

    /** Resolve a saved beneficiary to its destination wallet (owner-scoped). */
    UUID resolveDestinationWallet(UUID beneficiaryId, UUID ownerUserId);
}
