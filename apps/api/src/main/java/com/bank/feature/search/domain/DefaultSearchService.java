package com.bank.feature.search.domain;

import com.bank.feature.customers.domain.CustomerService;
import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.feature.ledger.persistence.LedgerEntry;
import com.bank.feature.ledger.persistence.LedgerEntryRepository;
import com.bank.feature.search.web.dto.TransactionSearchResult;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DefaultSearchService implements SearchService {

    private final CustomerService customers;
    private final LedgerEntryRepository ledger;

    public DefaultSearchService(CustomerService customers, LedgerEntryRepository ledger) {
        this.customers = customers;
        this.ledger = ledger;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerView> searchCustomers(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            throw new ApiException("SEARCH_QUERY_REQUIRED", "At least one query parameter must be provided", 400);
        }
        return customers.search(q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionSearchResult> searchTransactions(String referenceNumber,
                                                             UUID requestingUserId,
                                                             Pageable pageable) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            throw new ApiException("SEARCH_QUERY_REQUIRED", "referenceNumber is required", 400);
        }
        UUID txId;
        try {
            txId = UUID.fromString(referenceNumber);
        } catch (IllegalArgumentException e) {
            throw new ApiException("INVALID_REFERENCE", "referenceNumber must be a valid UUID", 400);
        }

        List<LedgerEntry> entries = ledger.findByTransactionId(txId);
        List<TransactionSearchResult> results = entries.stream()
                .map(TransactionSearchResult::of).toList();
        return new PageImpl<>(results, pageable, results.size());
    }
}
