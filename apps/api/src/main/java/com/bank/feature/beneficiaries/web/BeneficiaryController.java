package com.bank.feature.beneficiaries.web;

import com.bank.feature.beneficiaries.domain.BeneficiaryService;
import com.bank.feature.beneficiaries.web.dto.BeneficiaryView;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Beneficiaries", description = "Saved transfer destinations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/beneficiaries")
public class BeneficiaryController {

    public record CreateBeneficiaryRequest(@NotBlank String alias, @NotNull UUID destinationWalletId) {}
    public record UpdateBeneficiaryRequest(@NotBlank String alias) {}

    private final BeneficiaryService beneficiaries;
    private final CurrentUser currentUser;

    public BeneficiaryController(BeneficiaryService beneficiaries, CurrentUser currentUser) {
        this.beneficiaries = beneficiaries;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Save a new beneficiary")
    @PostMapping
    @PreAuthorize("hasAuthority('beneficiary:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryView create(@RequestBody CreateBeneficiaryRequest req) {
        UUID userId = currentUser.id().orElseThrow();
        return beneficiaries.create(userId, req.alias(), req.destinationWalletId());
    }

    @Operation(summary = "List saved beneficiaries")
    @GetMapping
    @PreAuthorize("hasAuthority('beneficiary:read')")
    public Page<BeneficiaryView> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        UUID userId = currentUser.id().orElseThrow();
        return beneficiaries.list(userId, pageable);
    }

    @Operation(summary = "Update a beneficiary alias")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('beneficiary:update')")
    public BeneficiaryView update(@PathVariable UUID id, @RequestBody UpdateBeneficiaryRequest req) {
        UUID userId = currentUser.id().orElseThrow();
        return beneficiaries.update(id, userId, req.alias());
    }

    @Operation(summary = "Remove a beneficiary")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('beneficiary:delete')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UUID userId = currentUser.id().orElseThrow();
        beneficiaries.delete(id, userId);
    }
}
