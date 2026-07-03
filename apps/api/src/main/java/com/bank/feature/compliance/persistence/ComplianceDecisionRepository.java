package com.bank.feature.compliance.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplianceDecisionRepository extends JpaRepository<ComplianceDecision, UUID> {}
