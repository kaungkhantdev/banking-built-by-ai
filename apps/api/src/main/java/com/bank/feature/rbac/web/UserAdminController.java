package com.bank.feature.rbac.web;

import com.bank.feature.rbac.domain.PermissionService;
import com.bank.feature.rbac.web.dto.UserWithRolesView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Admin", description = "Operator-only user and role management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/v1/admin/users")
public class UserAdminController {

    public record AssignRoleRequest(@NotBlank String role) {
    }

    private final PermissionService permissions;

    public UserAdminController(PermissionService permissions) {
        this.permissions = permissions;
    }

    @Operation(summary = "List all users with their assigned roles")
    @GetMapping
    @PreAuthorize("hasAuthority('user:assign-role')")
    public Page<UserWithRolesView> listUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return permissions.listUsers(pageable);
    }

    @Operation(summary = "Assign a role to a user")
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:assign-role')")
    public void assignRole(@PathVariable UUID id, @RequestBody AssignRoleRequest req) {
        permissions.assignRole(id, req.role());
    }
}
