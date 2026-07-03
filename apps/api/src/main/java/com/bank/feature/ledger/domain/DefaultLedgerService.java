package com.bank.feature.ledger.domain;

import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntry;
import com.bank.feature.ledger.persistence.LedgerEntryRepository;
import com.bank.shared.utils.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The single owner of {@code ledger_entries} (SRP for money correctness). All
 * writes happen inside the caller's transaction so the ledger commits atomically
 * with the rest of a money movement.
 */
@Service
public class DefaultLedgerService implements LedgerService {

    private final LedgerEntryRepository ledger;

    public DefaultLedgerService(LedgerEntryRepository ledger) {
        this.ledger = ledger;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void postDoubleEntry(UUID transactionId, UUID fromWallet, UUID toWallet,
                                BigDecimal amount, String currency, Instant at, String memo) {
        BigDecimal amt = Money.of(amount);
        ledger.save(new LedgerEntry(transactionId, fromWallet, Direction.DEBIT, amt, currency, at, memo));
        ledger.save(new LedgerEntry(transactionId, toWallet, Direction.CREDIT, amt, currency, at, memo));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry postLeg(UUID transactionId, UUID walletId, Direction direction,
                               BigDecimal amount, String currency, Instant at, String memo) {
        return ledger.save(new LedgerEntry(transactionId, walletId, direction,
                Money.of(amount), currency, at, memo));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal balanceOf(UUID walletId) {
        return Money.of(ledger.deriveBalance(walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> entriesOf(UUID transactionId) {
        return ledger.findByTransactionId(transactionId);
    }
}
