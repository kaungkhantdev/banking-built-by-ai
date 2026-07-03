package com.bank.feature.ledger.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /**
     * Balance derived purely from immutable facts: CREDIT adds, DEBIT subtracts.
     * There is no stored balance column to drift out of sync.
     */
    @Query("""
            select coalesce(sum(case when e.direction = com.bank.feature.ledger.persistence.Direction.CREDIT
                                     then e.amount else -e.amount end), 0)
            from LedgerEntry e
            where e.walletId = :walletId
            """)
    BigDecimal deriveBalance(@Param("walletId") UUID walletId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    List<LedgerEntry> findByWalletIdOrderByPostedAtDesc(UUID walletId);

    org.springframework.data.domain.Page<LedgerEntry> findByWalletId(UUID walletId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<LedgerEntry> findByWalletIdAndDirection(UUID walletId,
            com.bank.feature.ledger.persistence.Direction direction,
            org.springframework.data.domain.Pageable pageable);
}
