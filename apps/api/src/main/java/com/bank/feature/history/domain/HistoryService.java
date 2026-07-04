package com.bank.feature.history.domain;

import com.bank.feature.history.web.dto.TransactionView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface HistoryService {

    /**
     * Paged, filtered history with a running balance per row. Any filter may be
     * {@code null} to disable it. {@code direction} is the transaction "type".
     */
    Page<TransactionView> listForWallet(UUID walletId, String direction, String currency,
                                        Instant from, Instant to, Pageable pageable);

    /** All matching rows rendered as CSV (FR-12.4). */
    String exportCsv(UUID walletId, String direction, String currency, Instant from, Instant to);
}
