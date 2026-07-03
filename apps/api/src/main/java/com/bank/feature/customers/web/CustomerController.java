package com.bank.feature.customers.web;

import com.bank.feature.customers.domain.CustomerService;
import com.bank.feature.customers.web.dto.CustomerView;
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

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Customers", description = "Customer profile management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/customers")
public class CustomerController {

    public record CreateCustomerRequest(
            @NotNull UUID userId,
            @NotBlank String fullName,
            String phone,
            LocalDate dateOfBirth) {}

    public record UpdateCustomerRequest(String fullName, String phone) {}

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    @Operation(summary = "Create a customer profile")
    @PostMapping
    @PreAuthorize("hasAuthority('customer:create')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerView create(@RequestBody CreateCustomerRequest req) {
        return customers.create(req.userId(), req.fullName(), req.phone(), req.dateOfBirth());
    }

    @Operation(summary = "Get a customer by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:read')")
    public CustomerView get(@PathVariable UUID id) {
        return customers.get(id);
    }

    @Operation(summary = "Update a customer profile")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:update')")
    public CustomerView update(@PathVariable UUID id, @RequestBody UpdateCustomerRequest req) {
        return customers.update(id, req.fullName(), req.phone());
    }

    @Operation(summary = "Search customers")
    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    public Page<CustomerView> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return customers.search(q, pageable);
    }

    @Operation(summary = "Suspend a customer")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('customer:update')")
    public CustomerView suspend(@PathVariable UUID id) {
        return customers.suspend(id);
    }

    @Operation(summary = "Close a customer account")
    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('customer:update')")
    public CustomerView close(@PathVariable UUID id) {
        return customers.close(id);
    }
}
