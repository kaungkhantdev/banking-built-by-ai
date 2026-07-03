package com.bank.feature.history.domain;

import com.bank.feature.history.web.dto.TransactionView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HistoryService {

    Page<TransactionView> listForWallet(UUID walletId, String direction, Pageable pageable);
}
