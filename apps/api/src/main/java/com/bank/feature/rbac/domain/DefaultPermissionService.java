package com.bank.feature.rbac.domain;

import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.rbac.persistence.Permission;
import com.bank.feature.rbac.persistence.Role;
import com.bank.feature.rbac.persistence.RoleRepository;
import com.bank.feature.rbac.persistence.UserRole;
import com.bank.feature.rbac.persistence.UserRoleRepository;
import com.bank.feature.rbac.web.dto.UserWithRolesView;
import com.bank.shared.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves effective permissions by walking user → roles → permissions. Eagerly
 * fetched role permissions keep this a single query path; for larger permission
 * sets a JPQL projection can replace it without changing the port.
 */
@Service
public class DefaultPermissionService implements PermissionService {

    private final UserRoleRepository userRoles;
    private final RoleRepository roles;
    private final UserRepository users;

    public DefaultPermissionService(UserRoleRepository userRoles, RoleRepository roles, UserRepository users) {
        this.userRoles = userRoles;
        this.roles = roles;
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> permissionsFor(UUID userId) {
        Set<String> perms = new LinkedHashSet<>();
        for (UserRole ur : userRoles.findByUserId(userId)) {
            roles.findById(ur.getRoleId()).ifPresent(role -> {
                for (Permission p : role.getPermissions()) {
                    perms.add(p.getName());
                }
            });
        }
        return perms;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserWithRolesView> listUsers(Pageable pageable) {
        return users.findAll(pageable).map(user -> {
            List<String> roleNames = userRoles.findByUserId(user.getId()).stream()
                    .map(ur -> roles.findById(ur.getRoleId()).map(Role::getName).orElse("?"))
                    .toList();
            return new UserWithRolesView(user.getId(), user.getEmail(), user.isEnabled(), roleNames, user.getCreatedAt());
        });
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, String roleName) {
        Role role = roles.findByName(roleName)
                .orElseThrow(() -> new ApiException("ROLE_NOT_FOUND",
                        "Unknown role: " + roleName, 404));
        boolean already = userRoles.findByUserId(userId).stream()
                .anyMatch(ur -> ur.getRoleId().equals(role.getId()));
        if (!already) {
            userRoles.save(new UserRole(userId, role.getId()));
        }
    }
}
