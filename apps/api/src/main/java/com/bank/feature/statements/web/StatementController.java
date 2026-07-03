package com.bank.feature.statements.web;

import com.bank.feature.statements.domain.StatementService;
import com.bank.feature.statements.web.dto.StatementView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Statements", description = "Monthly account statements")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/accounts/{id}/statements")
public class StatementController {

    public record StatementRequest(@Min(2000) @Max(2100) int year,
                                   @Min(1) @Max(12) int month) {}

    private final StatementService statements;

    public StatementController(StatementService statements) {
        this.statements = statements;
    }

    @Operation(summary = "Request a statement for a period (async, returns 202)")
    @PostMapping
    @PreAuthorize("hasAuthority('account:manage')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StatementView request(@PathVariable UUID id, @RequestBody StatementRequest req) {
        return statements.request(id, req.year(), req.month());
    }

    @Operation(summary = "List available statements")
    @GetMapping
    @PreAuthorize("hasAuthority('account:read')")
    public List<StatementView> list(@PathVariable UUID id) {
        return statements.list(id);
    }

    @Operation(summary = "Download a statement PDF")
    @GetMapping("/{statementId}")
    @PreAuthorize("hasAuthority('account:read')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable UUID statementId) {
        byte[] pdf = statements.download(id, statementId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + statementId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
