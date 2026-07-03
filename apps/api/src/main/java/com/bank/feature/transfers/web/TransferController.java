package com.bank.feature.transfers.web;

import com.bank.feature.transfers.domain.TransferCommand;
import com.bank.feature.transfers.domain.TransferResult;
import com.bank.feature.transfers.domain.TransferService;
import com.bank.feature.transfers.web.dto.TransferRequest;
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

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @Operation(summary = "Initiate a wallet-to-wallet transfer (idempotent via Idempotency-Key header)")
    @PostMapping
    @PreAuthorize("hasAuthority('transfer:create')")
    public ResponseEntity<TransferResult> create(
            @RequestHeader("Idempotency-Key") @NotBlank String idemKey,
            @Valid @RequestBody TransferRequest req) {

        var cmd = new TransferCommand(UUID.randomUUID(), req.fromWalletId(),
                req.toWalletId(), req.amount(), idemKey, req.memo());
        TransferResult result = transfers.transfer(cmd);
        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result);
    }

    @Operation(summary = "Reverse a completed transfer")
    @PostMapping("/{transactionId}/reverse")
    @PreAuthorize("hasAuthority('transaction:reverse')")
    public TransferResult reverse(@PathVariable UUID transactionId,
                                  @RequestHeader(value = "X-Reason", required = false) String reason) {
        return transfers.reverse(transactionId, reason == null ? "unspecified" : reason);
    }
}
