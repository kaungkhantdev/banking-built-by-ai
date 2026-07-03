package com.bank.shared.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Accessor for the authenticated principal. The JWT subject is the user id;
 * unauthenticated/system contexts resolve to {@code "system"}.
 */
@Component
public class CurrentUser {

    /** Raw principal name (user UUID string, or "system"/"anonymous"). */
    public String name() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "system";
    }

    /** Authenticated user id, when the principal is a real UUID subject. */
    public Optional<UUID> id() {
        try {
            return Optional.of(UUID.fromString(name()));
        } catch (IllegalArgumentException notAUuid) {
            return Optional.empty();
        }
    }
}
