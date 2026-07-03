package com.bank.feature.ledger.domain;

import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Port: the only writer/reader of the double-entry ledger. */
public interface LedgerService {

    /** Append one balanced pair (debit + credit) under a shared transactionId. */
    void postDoubleEntry(UUID transactionId, UUID fromWallet, UUID toWallet,
                         BigDecimal amount, String currency, Instant at, String memo);

    /** Append a single leg (used by reversals to mirror each original leg). */
    LedgerEntry postLeg(UUID transactionId, UUID walletId, Direction direction,
                        BigDecimal amount, String currency, Instant at, String memo);

    BigDecimal balanceOf(UUID walletId);

    List<LedgerEntry> entriesOf(UUID transactionId);
}
