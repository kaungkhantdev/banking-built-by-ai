package com.bank.feature.search.web.dto;

import com.bank.feature.ledger.persistence.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionSearchResult(
        UUID transactionId,
        UUID walletId,
        String direction,
        BigDecimal amount,
        String currency,
        String memo,
        Instant postedAt
) {
    public static TransactionSearchResult of(LedgerEntry e) {
        return new TransactionSearchResult(e.getTransactionId(), e.getWalletId(),
                e.getDirection().name(), e.getAmount(), e.getCurrency(), e.getMemo(), e.getPostedAt());
    }
}
