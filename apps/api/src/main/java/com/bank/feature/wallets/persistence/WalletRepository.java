package com.bank.feature.wallets.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    List<Wallet> findByAccountId(UUID accountId);

    List<Wallet> findByAccountIdIn(Collection<UUID> accountIds);

    Optional<Wallet> findByAccountIdAndCurrency(UUID accountId, String currency);
}
