package com.bank.feature.kyc.web;

import com.bank.feature.kyc.domain.KycService;
import com.bank.feature.kyc.persistence.KycStatus;
import com.bank.feature.kyc.web.dto.KycCaseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "KYC", description = "Know-Your-Customer case management and document submission")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/kyc")
public class KycController {

    public record OpenCaseRequest(@NotNull UUID accountId) {
    }

    public record StatusView(UUID accountId, KycStatus status) {
    }

    private final KycService kyc;

    public KycController(KycService kyc) {
        this.kyc = kyc;
    }

    @Operation(summary = "List KYC cases (paginated), optionally filtered by status")
    @GetMapping
    @PreAuthorize("hasAuthority('kyc:read')")
    public Page<KycCaseView> list(
            @RequestParam(required = false) KycStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return kyc.list(status, pageable);
    }

    @Operation(summary = "Open a KYC case for an account")
    @PostMapping
    @PreAuthorize("hasAuthority('kyc:submit')")
    public UUID open(@RequestBody OpenCaseRequest req) {
        return kyc.openCase(req.accountId());
    }

    @Operation(summary = "Upload a KYC identity document")
    @PostMapping("/{accountId}/documents")
    @PreAuthorize("hasAuthority('kyc:submit')")
    public void submit(@PathVariable UUID accountId,
                       @RequestParam("file") MultipartFile file) throws IOException {
        kyc.submitDocument(accountId, file.getBytes(), file.getContentType());
    }

    @Operation(summary = "Get KYC status for an account")
    @GetMapping("/{accountId}/status")
    @PreAuthorize("hasAuthority('kyc:read')")
    public StatusView status(@PathVariable UUID accountId) {
        return new StatusView(accountId, kyc.statusOf(accountId));
    }
}
