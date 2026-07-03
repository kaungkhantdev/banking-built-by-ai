package com.bank.feature.search.domain;

import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.feature.search.web.dto.TransactionSearchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SearchService {

    Page<CustomerView> searchCustomers(String q, Pageable pageable);

    Page<TransactionSearchResult> searchTransactions(String referenceNumber, UUID requestingUserId, Pageable pageable);
}
