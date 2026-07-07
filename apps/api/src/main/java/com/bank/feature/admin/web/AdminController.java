package com.bank.feature.admin.web;

import com.bank.feature.accounts.web.dto.AccountView;
import com.bank.feature.admin.domain.AdminService;
import com.bank.feature.customers.domain.CustomerService;
import com.bank.feature.customers.web.dto.CustomerView;
import com.bank.feature.wallets.web.dto.WalletView;
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
    private final AdminService admin;

    public AdminController(CustomerService customers, AdminService admin) {
        this.customers = customers;
        this.admin = admin;
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
        return admin.freezeAccount(id);
    }

    @Operation(summary = "Freeze a wallet")
    @PostMapping("/wallets/{id}/freeze")
    @PreAuthorize("hasAuthority('wallet:manage')")
    public WalletView freezeWallet(@PathVariable UUID id) {
        return admin.freezeWallet(id);
    }

    @Operation(summary = "Unlock a locked-out user")
    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasAuthority('admin:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUser(@PathVariable UUID id) {
        admin.unlockUser(id);
    }
}
