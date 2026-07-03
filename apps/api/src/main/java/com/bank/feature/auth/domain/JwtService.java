package com.bank.feature.auth.domain;

import com.bank.feature.auth.persistence.User;

import java.util.Set;

/** Port: issue and verify RS256 access tokens. */
public interface JwtService {

    String issueAccessToken(User user, Set<String> permissions);

    /** @return the subject (user id) if the token is valid; throws otherwise. */
    String verifyAndGetSubject(String token);

    /** Permissions carried in the {@code perms} claim of a verified token. */
    Set<String> permissions(String token);
}
