package com.bank.feature.accounts.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByOwnerUserId(UUID ownerUserId);

    List<Account> findAllByOwnerUserId(UUID ownerUserId);
}
