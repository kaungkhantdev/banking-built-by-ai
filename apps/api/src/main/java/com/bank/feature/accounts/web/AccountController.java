package com.bank.feature.accounts.web;

import com.bank.feature.accounts.domain.AccountService;
import com.bank.feature.accounts.web.dto.AccountListItem;
import com.bank.feature.accounts.web.dto.AccountOverview;
import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.shared.exception.ApiException;
import com.bank.shared.utils.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Accounts", description = "Open and manage bank accounts")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/accounts")
public class AccountController {

    public record OpenAccountRequest(@NotNull UUID ownerUserId) {
    }

    private final AccountService accounts;
    private final CurrentUser currentUser;

    public AccountController(AccountService accounts, CurrentUser currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    @Operation(summary = "List all accounts (paginated)")
    @GetMapping
    @PreAuthorize("hasAuthority('account:manage')")
    public Page<AccountListItem> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return accounts.list(pageable);
    }

    @Operation(summary = "List the current user's own accounts")
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('wallet:read')")
    public List<AccountListItem> mine() {
        return accounts.listForUser(currentUserId());
    }

    @Operation(summary = "Open a new account for the current user (self-service)")
    @PostMapping("/me")
    @PreAuthorize("hasAuthority('wallet:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView openMine() {
        return accounts.open(currentUserId());
    }

    @Operation(summary = "Full detail for one of the current user's accounts")
    @GetMapping("/{id}/overview")
    @PreAuthorize("hasAuthority('wallet:read')")
    public AccountOverview overview(@PathVariable UUID id) {
        boolean isManager = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> "account:manage".equals(a.getAuthority()));
        return accounts.overview(id, currentUserId(), isManager);
    }

    private UUID currentUserId() {
        return currentUser.id()
                .orElseThrow(() -> new ApiException("AUTH_REQUIRED", "Authentication required", 401));
    }

    @Operation(summary = "Open a new account for a user")
    @PostMapping
    @PreAuthorize("hasAuthority('account:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView open(@RequestBody OpenAccountRequest req) {
        return accounts.open(req.ownerUserId());
    }

    @Operation(summary = "Activate a pending account")
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('account:manage')")
    public AccountView activate(@PathVariable UUID id) {
        return accounts.activate(id);
    }
}
