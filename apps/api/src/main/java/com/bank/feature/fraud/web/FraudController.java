package com.bank.feature.fraud.web;

import com.bank.feature.fraud.domain.FraudService;
import com.bank.feature.fraud.web.dto.FraudAlertView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Fraud", description = "Review and act on fraud alerts")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/fraud/alerts")
public class FraudController {

    private final FraudService fraud;

    public FraudController(FraudService fraud) {
        this.fraud = fraud;
    }

    @Operation(summary = "List fraud alerts (optionally filtered by status)")
    @GetMapping
    @PreAuthorize("hasAuthority('fraud:read')")
    public Page<FraudAlertView> list(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return fraud.list(status, pageable);
    }

    @Operation(summary = "Approve a held/flagged alert (clears it for its transfer)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('fraud:manage')")
    public FraudAlertView approve(@PathVariable UUID id) {
        return fraud.approve(id);
    }

    @Operation(summary = "Dismiss an alert as a false positive")
    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAuthority('fraud:manage')")
    public FraudAlertView dismiss(@PathVariable UUID id) {
        return fraud.dismiss(id);
    }
}
