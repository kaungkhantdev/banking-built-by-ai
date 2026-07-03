package com.bank.feature.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    List<PasswordHistory> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
