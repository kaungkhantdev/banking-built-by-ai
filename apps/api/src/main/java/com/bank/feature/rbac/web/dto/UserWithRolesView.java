package com.bank.feature.rbac.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserWithRolesView(UUID id, String email, boolean enabled, List<String> roles, Instant createdAt) {
}
