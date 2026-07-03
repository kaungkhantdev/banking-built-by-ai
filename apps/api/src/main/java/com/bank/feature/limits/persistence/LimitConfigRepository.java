package com.bank.feature.limits.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LimitConfigRepository extends JpaRepository<LimitConfig, UUID> {

    List<LimitConfig> findByCustomerTierAndActiveTrue(String customerTier);
}
