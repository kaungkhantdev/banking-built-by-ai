package com.bank.feature.beneficiaries.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, UUID> {

    Page<Beneficiary> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    boolean existsByOwnerUserIdAndDestinationWalletId(UUID ownerUserId, UUID destinationWalletId);
}
