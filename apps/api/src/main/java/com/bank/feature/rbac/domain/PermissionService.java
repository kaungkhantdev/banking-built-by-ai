package com.bank.feature.rbac.domain;

import com.bank.feature.rbac.web.dto.UserWithRolesView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

/** Port: resolve the effective permission set for a user. */
public interface PermissionService {

    /** Distinct permission names granted via the user's roles. */
    Set<String> permissionsFor(UUID userId);

    /** Assign a role to a user by role name. */
    void assignRole(UUID userId, String roleName);

    /** Paginated list of all users with their assigned roles. */
    Page<UserWithRolesView> listUsers(Pageable pageable);
}
