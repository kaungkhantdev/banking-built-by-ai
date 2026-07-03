package com.bank.feature.reporting.web;

import com.bank.feature.reporting.domain.ReportService;
import com.bank.feature.reporting.web.dto.ReportView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Reports", description = "Financial summary reports")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    public record ReportRequest(
            @NotBlank String reportType,
            @NotNull LocalDate from,
            @NotNull LocalDate to,
            @NotBlank String format) {}

    private final ReportService reports;
    private final CurrentUser currentUser;

    public ReportController(ReportService reports, CurrentUser currentUser) {
        this.reports = reports;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Request a report (async, returns 202)")
    @PostMapping
    @PreAuthorize("hasAuthority('report:generate')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ReportView request(@RequestBody ReportRequest req) {
        UUID userId = currentUser.id().orElseThrow();
        return reports.request(req.reportType(), req.from(), req.to(), req.format(), userId);
    }

    @Operation(summary = "List generated reports")
    @GetMapping
    @PreAuthorize("hasAuthority('report:read')")
    public List<ReportView> list() {
        return reports.list(currentUser.id().orElseThrow());
    }

    @Operation(summary = "Download a report")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('report:read')")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        byte[] data = reports.download(id, currentUser.id().orElseThrow());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}
