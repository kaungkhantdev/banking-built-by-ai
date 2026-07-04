package com.bank.feature.wallets.web;

import com.bank.feature.wallets.domain.WalletService;
import com.bank.feature.wallets.web.dto.BalanceView;
import com.bank.feature.wallets.web.dto.MyWalletView;
import com.bank.feature.wallets.web.dto.WalletListItem;
import com.bank.feature.wallets.web.dto.WalletView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Wallets", description = "Open wallets, read balances, and freeze wallets")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/wallets")
public class WalletController {

    public record OpenWalletRequest(@NotNull UUID accountId,
                                    @Pattern(regexp = "[A-Za-z]{3}") String currency) {
    }

    private final WalletService wallets;
    private final CurrentUser currentUser;

    public WalletController(WalletService wallets, CurrentUser currentUser) {
        this.wallets = wallets;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List all customer wallets (for operator pickers)")
    @GetMapping
    @PreAuthorize("hasAuthority('wallet:read')")
    public List<WalletListItem> list() {
        return wallets.list();
    }

    @Operation(summary = "List the signed-in customer's own wallets with balances")
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('wallet:read')")
    public List<MyWalletView> mine() {
        UUID userId = currentUser.id()
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED", "Authentication required", 401));
        return wallets.listMine(userId);
    }

    @Operation(summary = "Open a wallet for an account")
    @PostMapping
    @PreAuthorize("hasAuthority('wallet:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletView open(@RequestBody OpenWalletRequest req) {
        return wallets.open(req.accountId(), req.currency());
    }

    @Operation(summary = "Get current balance of a wallet")
    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('wallet:read')")
    public BalanceView balance(@PathVariable UUID id) {
        return new BalanceView(id, wallets.balance(id));
    }

    @Operation(summary = "Freeze a wallet")
    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAuthority('wallet:manage')")
    public WalletView freeze(@PathVariable UUID id) {
        return wallets.freeze(id);
    }

    @Operation(summary = "Unfreeze a wallet")
    @PostMapping("/{id}/unfreeze")
    @PreAuthorize("hasAuthority('wallet:manage')")
    public WalletView unfreeze(@PathVariable UUID id) {
        return wallets.unfreeze(id);
    }
}
