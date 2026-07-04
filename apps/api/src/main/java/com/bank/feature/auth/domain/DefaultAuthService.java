package com.bank.feature.auth.domain;

import com.bank.feature.auth.persistence.RefreshToken;
import com.bank.feature.auth.persistence.RefreshTokenRepository;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.auth.web.dto.RegisterRequest;
import com.bank.feature.auth.web.dto.UserView;
import com.bank.feature.audit.domain.AuditService;
import com.bank.feature.rbac.domain.PermissionService;
import com.bank.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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
    private final AuditService audit;
    private final Duration refreshTtl;
    private final int maxFailedLogins;
    private final Duration lockDuration;

    public DefaultAuthService(UserRepository users,
                              RefreshTokenRepository refreshTokens,
                              PermissionService permissions,
                              JwtService jwt,
                              PasswordEncoder encoder,
                              AuditService audit,
                              @Value("${security.jwt.refresh-ttl:P30D}") Duration refreshTtl,
                              @Value("${security.login.max-failed-attempts:5}") int maxFailedLogins,
                              @Value("${security.login.lock-duration:PT15M}") Duration lockDuration) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.permissions = permissions;
        this.jwt = jwt;
        this.encoder = encoder;
        this.audit = audit;
        this.refreshTtl = refreshTtl;
        this.maxFailedLogins = maxFailedLogins;
        this.lockDuration = lockDuration;
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
        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            // No account — audit the attempt without leaking existence to the caller.
            audit.write(email, "auth:login-failed", null, "unknown-account");
            throw new ApiException("AUTH_INVALID", "Invalid credentials", 401);
        }

        User user = found.get();

        if (user.isLocked()) {
            audit.write(user.getId().toString(), "auth:login-blocked", null, "account-locked");
            throw new ApiException("AUTH_LOCKED",
                    "Account temporarily locked due to failed logins", 423);
        }
        if (!user.isEnabled()) {
            audit.write(user.getId().toString(), "auth:login-blocked", null, "account-disabled");
            throw new ApiException("AUTH_INVALID", "Invalid credentials", 401);
        }
        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            user.recordFailedLogin(maxFailedLogins, lockDuration);   // FR-14.4
            audit.write(user.getId().toString(), "auth:login-failed", null,
                    "locked=" + user.isLocked());
            throw new ApiException("AUTH_INVALID", "Invalid credentials", 401);
        }

        user.resetFailedLogins();
        audit.write(user.getId().toString(), "auth:login-success", null, null);   // FR-14.5
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
