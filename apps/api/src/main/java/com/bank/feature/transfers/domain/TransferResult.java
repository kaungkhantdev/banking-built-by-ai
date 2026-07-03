package com.bank.feature.transfers.domain;

import java.util.UUID;

/**
 * Result of a money operation. {@code replayed} is true when an idempotent retry
 * matched a prior key — the controller returns 200 instead of 201 in that case.
 */
public record TransferResult(UUID transactionId, String status, boolean replayed) {

    public static TransferResult posted(UUID txId) {
        return new TransferResult(txId, "POSTED", false);
    }

    public static TransferResult replayed(UUID txId) {
        return new TransferResult(txId, "POSTED", true);
    }
}
