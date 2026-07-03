package com.bank.feature.mfa.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {

    List<RecoveryCode> findByUserId(UUID userId);

    Optional<RecoveryCode> findByUserIdAndCodeHashAndUsedFalse(UUID userId, String codeHash);

    long countByUserIdAndUsedFalse(UUID userId);

    void deleteByUserId(UUID userId);
}
