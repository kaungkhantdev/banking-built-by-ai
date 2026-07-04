package com.bank.feature.accounts.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Customer-facing account detail: the account itself, its holder/KYC status,
 * the wallets under it (with derived balances) and a handful of recent
 * transactions merged across those wallets. Assembled in a single read so the
 * mobile detail screen needs only one round trip.
 */
public record AccountOverview(
        UUID id,
        UUID ownerUserId,
        String ownerEmail,
        String status,
        Instant createdAt,
        String kycStatus,
        List<WalletBrief> wallets,
        List<TxBrief> recentTransactions
) {
    public record WalletBrief(UUID id, String currency, String status, BigDecimal balance) {
    }

    public record TxBrief(UUID id, UUID walletId, String currency, String direction,
                          BigDecimal amount, String memo, Instant postedAt) {
    }
}
