package com.bank.feature.admin.web;

import com.bank.feature.accounts.domain.AccountService;
import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.customers.domain.CustomerService;
import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.feature.wallets.domain.WalletService;
import com.bank.feature.wallets.web.dto.WalletView;
import com.bank.shared.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin", description = "Back-office operator console")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final CustomerService customers;
    private final AccountService accounts;
    private final WalletService wallets;
    private final UserRepository users;

    public AdminController(CustomerService customers, AccountService accounts,
                            WalletService wallets, UserRepository users) {
        this.customers = customers;
        this.accounts = accounts;
        this.wallets = wallets;
        this.users = users;
    }

    @Operation(summary = "Search customers")
    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('admin:read')")
    public Page<CustomerView> searchCustomers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return customers.search(q, pageable);
    }

    @Operation(summary = "Freeze an account")
    @PostMapping("/accounts/{id}/freeze")
    @PreAuthorize("hasAuthority('account:manage')")
    public AccountView freezeAccount(@PathVariable UUID id) {
        return accounts.freeze(id);
    }

    @Operation(summary = "Freeze a wallet")
    @PostMapping("/wallets/{id}/freeze")
    @PreAuthorize("hasAuthority('wallet:manage')")
    public WalletView freezeWallet(@PathVariable UUID id) {
        return wallets.freeze(id);
    }

    @Operation(summary = "Unlock a locked-out user")
    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAuthority('admin:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUser(@PathVariable UUID id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "Unknown user", 404));
        user.setEnabled(true);
        users.save(user);
    }
}
