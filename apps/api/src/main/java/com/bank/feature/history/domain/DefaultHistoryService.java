package com.bank.feature.history.domain;

import com.bank.feature.history.web.dto.TransactionView;
import com.bank.feature.ledger.persistence.Direction;
import com.bank.feature.ledger.persistence.LedgerEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultHistoryService implements HistoryService {

    private final LedgerEntryRepository ledger;

    public DefaultHistoryService(LedgerEntryRepository ledger) {
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionView> listForWallet(UUID walletId, String direction, Pageable pageable) {
        if (direction != null && !direction.isBlank()) {
            Direction dir = Direction.valueOf(direction.toUpperCase());
            return ledger.findByWalletIdAndDirection(walletId, dir, pageable).map(TransactionView::of);
        }
        return ledger.findByWalletId(walletId, pageable).map(TransactionView::of);
    }
}
