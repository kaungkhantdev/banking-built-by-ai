package com.bank.feature.transfers.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Destination is given either directly ({@code toWalletId}) or indirectly via a
 * saved {@code beneficiaryId}; exactly one must be present (validated in the
 * controller). {@code beneficiaryId} is resolved to its destination wallet.
 */
public record TransferRequest(
        @NotNull UUID fromWalletId,
        UUID toWalletId,
        UUID beneficiaryId,
        @NotNull @Positive BigDecimal amount,
        String memo) {
}
