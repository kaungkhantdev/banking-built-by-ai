package com.bank.feature.history.web.dto;

import com.bank.feature.history.persistence.TransactionExport;

import java.time.Instant;
import java.util.UUID;

public record TransactionExportView(
        UUID id,
        UUID walletId,
        String status,
        Integer rowCount,
        UUID fileId,
        String error,
        Instant createdAt) {

    public static TransactionExportView of(TransactionExport e) {
        return new TransactionExportView(e.getId(), e.getWalletId(), e.getStatus(),
                e.getRowCount(), e.getFileId(), e.getError(), e.getCreatedAt());
    }
}
