package com.bank.feature.auth.domain;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.bank.config.KeyProvider;
import com.bank.feature.auth.persistence.User;
import com.bank.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RS256 token issue/verify. The private key signs; the public key verifies.
 * Permissions are embedded in a {@code perms} claim so authorization needs no
 * DB lookup per request.
 */
@Service
public class DefaultJwtService implements JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Duration accessTtl;

    public DefaultJwtService(KeyProvider keys,
                             @Value("${security.jwt.access-ttl:PT10M}") Duration accessTtl) {
        this.algorithm = Algorithm.RSA256(keys.publicKey(), keys.privateKey());
        this.verifier = JWT.require(algorithm).withIssuer("bank-core").build();
        this.accessTtl = accessTtl;
    }

    @Override
    public String issueAccessToken(User user, Set<String> permissions) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer("bank-core")
                .withClaim("email", user.getEmail())
                .withArrayClaim("perms", permissions.toArray(String[]::new))
                .withIssuedAt(now)
                .withExpiresAt(now.plus(accessTtl))
                .sign(algorithm);
    }

    @Override
    public String verifyAndGetSubject(String token) {
        return decode(token).getSubject();
    }

    @Override
    public Set<String> permissions(String token) {
        String[] perms = decode(token).getClaim("perms").asArray(String.class);
        return perms == null ? Set.of() : new LinkedHashSet<>(Arrays.asList(perms));
    }

    private DecodedJWT decode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new ApiException("AUTH_TOKEN_INVALID", "Invalid or expired token", 401);
        }
    }
}
