package com.bank.feature.history.domain;

import com.bank.feature.history.persistence.TransactionExport;
import com.bank.feature.history.persistence.TransactionExportRepository;
import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.storage.domain.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Background worker for transaction exports. Runs off the request thread (@Async
 * on a separate bean so the proxy actually applies), streams the CSV, and stores
 * it — encrypted at rest — via the storage feature. Status updates use
 * {@code repository.save}, which is transactional on its own, so no ambient
 * transaction is required here.
 */
@Component
public class TransactionExportGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionExportGenerator.class);

    private final TransactionExportRepository exports;
    private final TransactionCsvWriter csvWriter;
    private final StorageService storage;

    public TransactionExportGenerator(TransactionExportRepository exports,
                                      TransactionCsvWriter csvWriter,
                                      StorageService storage) {
        this.exports = exports;
        this.csvWriter = csvWriter;
        this.storage = storage;
    }

    @Async("exportExecutor")
    public void generate(UUID jobId) {
        TransactionExport job = exports.findById(jobId).orElse(null);
        if (job == null) return;
        try {
            Direction dir = parseDirection(job.getDirection());
            TransactionCsvWriter.Csv csv = csvWriter.build(
                    job.getWalletId(), dir, job.getCurrency(), job.getFromTs(), job.getToTs());

            String name = "transactions-" + job.getWalletId() + "-" + jobId + ".csv";
            StorageService.UploadUrlResponse up =
                    storage.getUploadUrl(job.getOwnerUserId(), name, "text/csv");
            storage.uploadContent(up.fileId(), job.getOwnerUserId(), csv.bytes());

            job.markReady(up.fileId(), (int) csv.rowCount());
            exports.save(job);
            log.info("[EXPORT] {} ready ({} rows -> file {})", jobId, csv.rowCount(), up.fileId());
        } catch (Exception e) {
            job.markFailed(e.getMessage());
            exports.save(job);
            log.warn("[EXPORT] {} failed: {}", jobId, e.getMessage());
        }
    }

    private static Direction parseDirection(String direction) {
        return (direction == null || direction.isBlank())
                ? null : Direction.valueOf(direction.toUpperCase());
    }
}
