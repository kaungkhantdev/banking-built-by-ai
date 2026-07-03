package com.bank.feature.transfers.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Internal command for a transfer. {@code transactionId} groups the legs. */
public record TransferCommand(
        UUID transactionId,
        UUID fromWalletId,
        UUID toWalletId,
        BigDecimal amount,
        String idempotencyKey,
        String memo) {
}
