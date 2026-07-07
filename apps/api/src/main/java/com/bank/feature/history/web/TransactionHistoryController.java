package com.bank.feature.history.web;

import com.bank.feature.history.domain.HistoryService;
import com.bank.feature.history.domain.TransactionExportService;
import com.bank.feature.history.web.dto.TransactionExportView;
import com.bank.feature.history.web.dto.TransactionView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Transaction History", description = "Paginated ledger view per wallet")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/wallets/{id}/transactions")
public class TransactionHistoryController {

    private final HistoryService history;
    private final TransactionExportService exports;
    private final CurrentUser currentUser;

    public TransactionHistoryController(HistoryService history,
                                        TransactionExportService exports,
                                        CurrentUser currentUser) {
        this.history = history;
        this.exports = exports;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List transactions for a wallet (filterable, with running balance)")
    @GetMapping
    @PreAuthorize("hasAuthority('transfer:read')")
    public Page<TransactionView> list(
            @PathVariable UUID id,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "postedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return history.listForWallet(id, direction, currency, from, to, pageable);
    }

    @Operation(summary = "Export a small result set as CSV synchronously (bounded — use /exports for large sets)")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('transfer:read')")
    public ResponseEntity<String> exportCsv(
            @PathVariable UUID id,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        String csv = history.exportCsv(id, direction, currency, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transactions-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @Operation(summary = "Queue an async CSV export (returns 202; poll status, then download)")
    @PostMapping("/exports")
    @PreAuthorize("hasAuthority('transfer:read')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TransactionExportView requestExport(
            @PathVariable UUID id,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return exports.request(currentUser.id().orElseThrow(), id, direction, currency, from, to);
    }

    @Operation(summary = "Poll export job status")
    @GetMapping("/exports/{exportId}")
    @PreAuthorize("hasAuthority('transfer:read')")
    public TransactionExportView exportStatus(@PathVariable UUID id, @PathVariable UUID exportId) {
        return exports.get(currentUser.id().orElseThrow(), exportId);
    }

    @Operation(summary = "Download a completed export")
    @GetMapping("/exports/{exportId}/download")
    @PreAuthorize("hasAuthority('transfer:read')")
    public ResponseEntity<byte[]> downloadExport(@PathVariable UUID id, @PathVariable UUID exportId) {
        byte[] csv = exports.download(currentUser.id().orElseThrow(), exportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"export-" + exportId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
