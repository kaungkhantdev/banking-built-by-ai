package com.bank.feature.auth.web.dto;

import com.bank.feature.auth.persistence.User;

import java.util.UUID;

/** Safe projection of a user — never exposes the password hash. */
public record UserView(UUID id, String email, boolean enabled) {

    public static UserView of(User u) {
        return new UserView(u.getId(), u.getEmail(), u.isEnabled());
    }
}
