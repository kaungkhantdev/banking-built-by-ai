package com.bank.feature.history.web;

import com.bank.feature.history.domain.HistoryService;
import com.bank.feature.history.web.dto.TransactionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Transaction History", description = "Paginated ledger view per wallet")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/wallets/{id}/transactions")
public class TransactionHistoryController {

    private final HistoryService history;

    public TransactionHistoryController(HistoryService history) {
        this.history = history;
    }

    @Operation(summary = "List transactions for a wallet")
    @GetMapping
    @PreAuthorize("hasAuthority('transfer:read')")
    public Page<TransactionView> list(
            @PathVariable UUID id,
            @RequestParam(required = false) String direction,
            @PageableDefault(size = 20, sort = "postedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return history.listForWallet(id, direction, pageable);
    }
}
