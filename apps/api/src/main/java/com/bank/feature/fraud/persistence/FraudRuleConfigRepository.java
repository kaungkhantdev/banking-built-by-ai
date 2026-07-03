package com.bank.feature.fraud.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudRuleConfigRepository extends JpaRepository<FraudRuleConfig, UUID> {

    List<FraudRuleConfig> findByActiveTrue();
}
