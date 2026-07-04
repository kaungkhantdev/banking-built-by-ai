package com.bank.feature.ledger.persistence;

import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    Page<LedgerEntry> findByWalletId(UUID walletId, Pageable pageable);

    Page<LedgerEntry> findByWalletIdAndDirection(UUID walletId, Direction direction, Pageable pageable);

    /**
     * Filtered history (FR-12.3). Every filter is optional — a {@code null} param
     * disables that predicate. {@code direction} doubles as the "type" filter.
     */
    @Query("""
            select e from LedgerEntry e
            where e.walletId = :walletId
              and (:direction is null or e.direction = :direction)
              and (:currency is null or e.currency = :currency)
              and (cast(:from as Instant) is null or e.postedAt >= :from)
              and (cast(:to as Instant) is null or e.postedAt <= :to)
            """)
    Page<LedgerEntry> search(@Param("walletId") UUID walletId,
                             @Param("direction") Direction direction,
                             @Param("currency") String currency,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);

    /** Non-paged variant of {@link #search} for CSV export (FR-12.4). */
    @Query("""
            select e from LedgerEntry e
            where e.walletId = :walletId
              and (:direction is null or e.direction = :direction)
              and (:currency is null or e.currency = :currency)
              and (cast(:from as Instant) is null or e.postedAt >= :from)
              and (cast(:to as Instant) is null or e.postedAt <= :to)
            order by e.postedAt asc
            """)
    List<LedgerEntry> searchAll(@Param("walletId") UUID walletId,
                                @Param("direction") Direction direction,
                                @Param("currency") String currency,
                                @Param("from") Instant from,
                                @Param("to") Instant to);

    /**
     * Cursor-streamed variant for large async exports — rows arrive lazily with a
     * bounded JDBC fetch size so the worker never materializes the whole result
     * set. Must be consumed inside a read-only transaction and closed.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "1000"))
    @Query("""
            select e from LedgerEntry e
            where e.walletId = :walletId
              and (:direction is null or e.direction = :direction)
              and (:currency is null or e.currency = :currency)
              and (cast(:from as Instant) is null or e.postedAt >= :from)
              and (cast(:to as Instant) is null or e.postedAt <= :to)
            order by e.postedAt asc
            """)
    Stream<LedgerEntry> streamSearch(@Param("walletId") UUID walletId,
                                     @Param("direction") Direction direction,
                                     @Param("currency") String currency,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to);

    /**
     * Signed balance of a wallet as of an instant (inclusive) — used to compute
     * the running balance at each transaction (FR-12.5).
     */
    @Query("""
            select coalesce(sum(case when e.direction = com.bank.feature.ledger.persistence.Direction.CREDIT
                                     then e.amount else -e.amount end), 0)
            from LedgerEntry e
            where e.walletId = :walletId and e.postedAt <= :at
            """)
    BigDecimal signedSumAsOf(@Param("walletId") UUID walletId, @Param("at") Instant at);
}
