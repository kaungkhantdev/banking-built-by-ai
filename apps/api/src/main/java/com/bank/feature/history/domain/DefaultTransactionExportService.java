package com.bank.feature.history.domain;

import com.bank.feature.history.persistence.TransactionExport;
import com.bank.feature.history.persistence.TransactionExportRepository;
import com.bank.feature.history.web.dto.TransactionExportView;
import com.bank.feature.storage.domain.StorageService;
import com.bank.shared.exception.ApiException;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DefaultTransactionExportService implements TransactionExportService {

    private final TransactionExportRepository exports;
    private final TransactionExportGenerator generator;
    private final StorageService storage;

    public DefaultTransactionExportService(TransactionExportRepository exports,
                                           TransactionExportGenerator generator,
                                           StorageService storage) {
        this.exports = exports;
        this.generator = generator;
        this.storage = storage;
    }

    @Override
    public TransactionExportView request(UUID ownerUserId, UUID walletId, String direction,
                                         String currency, Instant from, Instant to) {
        String ccy = (currency == null || currency.isBlank()) ? null : currency.toUpperCase();
        // repository.save is transactional on its own, so the row is committed
        // before the async worker starts — no read-before-commit race.
        TransactionExport job = exports.save(
                new TransactionExport(ownerUserId, walletId, direction, ccy, from, to));
        try {
            generator.generate(job.getId());
        } catch (TaskRejectedException busy) {
            // Export pool saturated — fail fast and cleanly rather than leaving the
            // job stuck in QUEUED. The client sees FAILED on poll and can retry.
            job.markFailed("Export capacity reached, please retry shortly");
            exports.save(job);
        }
        return TransactionExportView.of(job);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionExportView get(UUID ownerUserId, UUID exportId) {
        return TransactionExportView.of(require(ownerUserId, exportId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] download(UUID ownerUserId, UUID exportId) {
        TransactionExport job = require(ownerUserId, exportId);
        if (!"READY".equals(job.getStatus()) || job.getFileId() == null) {
            throw new ApiException("EXPORT_NOT_READY", "Export is not ready yet", 202);
        }
        return storage.getContent(job.getFileId(), ownerUserId);
    }

    private TransactionExport require(UUID ownerUserId, UUID exportId) {
        TransactionExport job = exports.findById(exportId)
                .orElseThrow(() -> new ApiException("EXPORT_NOT_FOUND", "Export not found", 404));
        if (!job.getOwnerUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "You do not own this export", 403);
        }
        return job;
    }
}
