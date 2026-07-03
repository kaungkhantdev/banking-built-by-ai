package com.bank.feature.auth.domain;

import com.bank.feature.auth.persistence.RefreshToken;
import com.bank.feature.auth.persistence.RefreshTokenRepository;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.auth.web.dto.RegisterRequest;
import com.bank.feature.auth.web.dto.UserView;
import com.bank.feature.rbac.domain.PermissionService;
import com.bank.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Registration, login, and refresh-token rotation with theft detection.
 *
 * <p>On refresh, a presented token that is already ROTATED/REVOKED (or expired)
 * means replay — we cannot tell a buggy client from an attacker, so we revoke
 * the entire session family and fail safe.
 */
@Service
public class DefaultAuthService implements AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PermissionService permissions;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final Duration refreshTtl;

    public DefaultAuthService(UserRepository users,
                              RefreshTokenRepository refreshTokens,
                              PermissionService permissions,
                              JwtService jwt,
                              PasswordEncoder encoder,
                              @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.permissions = permissions;
        this.jwt = jwt;
        this.encoder = encoder;
        this.refreshTtl = refreshTtl;
    }

    @Override
    @Transactional
    public UserView register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new ApiException("EMAIL_TAKEN", "Email already registered", 409);
        }
        User user = users.save(new User(request.email(), encoder.encode(request.password())));
        return UserView.of(user);
    }

    @Override
    @Transactional
    public TokenPair login(String email, String rawPassword) {
        User user = users.findByEmail(email)
                .filter(u -> u.isEnabled())
                .filter(u -> encoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException("AUTH_INVALID", "Invalid credentials", 401));
        return issuePair(user, UUID.randomUUID(), null);   // new session family
    }

    @Override
    @Transactional
    public TokenPair refresh(String presentedSecret) {
        String hash = Tokens.sha256Hex(presentedSecret);
        RefreshToken token = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("AUTH_REFRESH_UNKNOWN", "Unknown token", 401));

        if (token.getStatus() != RefreshToken.Status.ACTIVE
                || token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.revokeSession(token.getSessionId());   // reuse/expiry → kill family
            throw new ApiException("AUTH_REFRESH_REUSE", "Refresh token reuse detected", 401);
        }

        token.setStatus(RefreshToken.Status.ROTATED);           // single-use
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new ApiException("AUTH_INVALID", "Unknown user", 401));
        return issuePair(user, token.getSessionId(), token.getId());
    }

    private TokenPair issuePair(User user, UUID sessionId, UUID previousId) {
        Set<String> perms = permissions.permissionsFor(user.getId());
        String access = jwt.issueAccessToken(user, perms);

        String secret = Tokens.newSecret();
        refreshTokens.save(new RefreshToken(
                user.getId(), sessionId, Tokens.sha256Hex(secret),
                previousId, Instant.now().plus(refreshTtl)));
        return new TokenPair(access, secret);
    }
}
