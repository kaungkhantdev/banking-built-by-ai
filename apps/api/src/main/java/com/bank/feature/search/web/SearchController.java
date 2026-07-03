package com.bank.feature.search.web;

import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.feature.search.domain.SearchService;
import com.bank.feature.search.web.dto.TransactionSearchResult;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "Unified search for customers and transactions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/search")
public class SearchController {

    private final SearchService search;
    private final CurrentUser currentUser;

    public SearchController(SearchService search, CurrentUser currentUser) {
        this.search = search;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Search customers by name, email, or phone")
    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('customer:read')")
    public Page<CustomerView> customers(
            @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return search.searchCustomers(q, pageable);
    }

    @Operation(summary = "Search transactions by reference number")
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('transfer:read')")
    public Page<TransactionSearchResult> transactions(
            @RequestParam String ref,
            @PageableDefault(size = 20) Pageable pageable) {
        return search.searchTransactions(ref, currentUser.id().orElseThrow(), pageable);
    }
}
