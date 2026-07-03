package com.bank.feature.fees.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeeConfigRepository extends JpaRepository<FeeConfig, UUID> {

    Optional<FeeConfig> findByTransferTypeAndCustomerTierAndActiveTrue(
            String transferType, String customerTier);
}
