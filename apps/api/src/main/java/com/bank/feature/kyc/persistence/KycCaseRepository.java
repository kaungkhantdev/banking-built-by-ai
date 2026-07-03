package com.bank.feature.kyc.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KycCaseRepository extends JpaRepository<KycCase, UUID> {

    Optional<KycCase> findByAccountId(UUID accountId);

    Page<KycCase> findByStatus(KycStatus status, Pageable pageable);
}
