package com.bank.feature.audit.web;

import com.bank.feature.audit.persistence.AuditRecord;
import com.bank.feature.audit.persistence.AuditRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit", description = "Auditor-only query over the append-only audit trail")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    private final AuditRecordRepository repo;

    public AuditController(AuditRecordRepository repo) {
        this.repo = repo;
    }

    @Operation(summary = "Search audit records by actor or action")
    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public Page<AuditRecord> search(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        return repo.search(actor, action, pageable);
    }
}
