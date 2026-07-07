package com.bank.feature.history.web.dto;

import com.bank.feature.ledger.persistence.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionView(
        UUID id,
        UUID transactionId,
        UUID walletId,
        String direction,
        BigDecimal amount,
        String currency,
        String memo,
        Instant postedAt,
        BigDecimal runningBalance
) {
    public static TransactionView of(LedgerEntry e) {
        return of(e, null);
    }

    public static TransactionView of(LedgerEntry e, BigDecimal runningBalance) {
        return new TransactionView(e.getId(), e.getTransactionId(), e.getWalletId(),
                e.getDirection().name(), e.getAmount(), e.getCurrency(), e.getMemo(),
                e.getPostedAt(), runningBalance);
    }
}
