package com.bank.feature.history.domain;

import com.bank.feature.history.web.dto.TransactionExportView;

import java.time.Instant;
import java.util.UUID;

public interface TransactionExportService {

    /** Queue an export and return immediately (caller responds 202). */
    TransactionExportView request(UUID ownerUserId, UUID walletId, String direction,
                                  String currency, Instant from, Instant to);

    /** Poll job status (owner-scoped). */
    TransactionExportView get(UUID ownerUserId, UUID exportId);

    /** Fetch the generated CSV bytes once the job is READY (owner-scoped). */
    byte[] download(UUID ownerUserId, UUID exportId);
}
