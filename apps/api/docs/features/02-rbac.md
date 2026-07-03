# Feature 2 — RBAC (Role-Based Access Control)

**Package:** `com.bank.feature.rbac` · **Endpoints:** `/v1/admin/users/*`

## Purpose
Resolve a user's effective permissions and gate every operation with fine-grained
authorities. The server is the enforcement point; the JWT is the carrier.

## Layout
```
rbac/
├── web/        UserAdminController (AssignRoleRequest)
├── domain/     PermissionService + DefaultPermissionService
└── persistence/ Role, Permission, UserRole, + repositories
```

## Model
- **Permission** — a fine-grained authority string, e.g. `transfer:create`,
  `wallet:manage`, `audit:read`.
- **Role** — a named bundle of permissions (`CUSTOMER`, `OPERATOR`, `AUDITOR`).
- **UserRole** — assignment of a role to a user.

Seeded in `V6__seed_rbac.sql`. The permission names there are exactly the strings
used in `@PreAuthorize("hasAuthority('...')")` across the controllers.

## Endpoints
| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| POST | `/v1/admin/users/{id}/roles` | `user:assign-role` | Assign a role to a user |

## Key rules
- **Default-deny:** `SecurityConfig` requires authentication for every non-public
  route; method-level `@PreAuthorize` adds the specific permission check.
- **No per-request DB hit:** effective permissions are computed at login
  (`DefaultPermissionService.permissionsFor`) and embedded in the JWT `perms`
  claim. `JwtAuthFilter` turns them into Spring `GrantedAuthority`s.

## Code pointers
- Resolution: `DefaultPermissionService.permissionsFor` (user → roles → permissions)
- Assignment: `DefaultPermissionService.assignRole`
- Enforcement wiring: `config/SecurityConfig` (`@EnableMethodSecurity`)

## Related requirements
FR-2.*, NFR-1.4
