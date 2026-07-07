package com.bank.feature.transfers.web;

import com.bank.feature.beneficiaries.domain.BeneficiaryService;
import com.bank.feature.mfa.domain.MfaService;
import com.bank.feature.transfers.domain.TransferCommand;
import com.bank.feature.transfers.domain.TransferResult;
import com.bank.feature.transfers.domain.TransferService;
import com.bank.feature.transfers.web.dto.TransferRequest;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Transfers", description = "Initiate and reverse wallet-to-wallet transfers")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/transfers")
public class TransferController {

    private final TransferService transfers;
    private final BeneficiaryService beneficiaries;
    private final MfaService mfa;
    private final CurrentUser currentUser;

    public TransferController(TransferService transfers,
                              BeneficiaryService beneficiaries,
                              MfaService mfa,
                              CurrentUser currentUser) {
        this.transfers = transfers;
        this.beneficiaries = beneficiaries;
        this.mfa = mfa;
        this.currentUser = currentUser;
    }

    @Operation(summary = "Initiate a wallet-to-wallet transfer (idempotent via Idempotency-Key header)")
    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create')")
    public ResponseEntity<TransferResult> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idemKey,
            @Valid @RequestBody TransferRequest req) {

        UUID toWalletId = resolveDestination(req);
        var cmd = new TransferCommand(UUID.randomUUID(), req.fromWalletId(),
                toWalletId, req.amount(), idemKey, req.memo());
        TransferResult result = transfers.transfer(cmd);
        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result);
    }

    /** Destination is either a direct wallet id or a saved beneficiary — exactly one. */
    private UUID resolveDestination(TransferRequest req) {
        boolean hasWallet = req.toWalletId() != null;
        boolean hasBeneficiary = req.beneficiaryId() != null;
        if (hasWallet == hasBeneficiary) {
            throw new ApiException("DESTINATION_INVALID",
                    "Provide exactly one of toWalletId or beneficiaryId", 422);
        }
        if (hasWallet) {
            return req.toWalletId();
        }
        UUID userId = currentUser.id().orElseThrow();
        return beneficiaries.resolveDestinationWallet(req.beneficiaryId(), userId);
    }

    @Operation(summary = "Reverse a completed transfer (MFA step-up required if enrolled)")
    @PostMapping("/{transactionId}/reverse")
    @PreAuthorize("hasAuthority('transaction:reverse')")
    public TransferResult reverse(@PathVariable UUID transactionId,
                                  @RequestHeader(value = "X-Reason", required = false) String reason,
                                  @RequestHeader(value = "X-MFA-Code", required = false) String mfaCode) {
        // FR-15.4: reversing money is sensitive — require MFA re-auth when enrolled.
        mfa.assertStepUp(currentUser.name(), mfaCode);
        return transfers.reverse(transactionId, reason == null ? "unspecified" : reason);
    }
}
