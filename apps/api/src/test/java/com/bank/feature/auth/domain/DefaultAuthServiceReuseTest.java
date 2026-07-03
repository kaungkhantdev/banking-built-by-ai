package com.bank.feature.auth.domain;

import com.bank.feature.auth.persistence.RefreshToken;
import com.bank.feature.auth.persistence.RefreshTokenRepository;
import com.bank.feature.auth.persistence.User;
import com.bank.feature.auth.persistence.UserRepository;
import com.bank.feature.rbac.domain.PermissionService;
import com.bank.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security invariant: presenting an already-ROTATED refresh token is treated as
 * theft — the whole session family is revoked and the call fails. Infra-free.
 */
class DefaultAuthServiceReuseTest {

    private RefreshTokenRepository refreshTokens;
    private DefaultAuthService service;

    @BeforeEach
    void setUp() {
        UserRepository users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);
        PermissionService permissions = mock(PermissionService.class);
        JwtService jwt = mock(JwtService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(permissions.permissionsFor(any())).thenReturn(Set.of());
        when(jwt.issueAccessToken(any(), any())).thenReturn("access-token");
        service = new DefaultAuthService(users, refreshTokens, permissions, jwt, encoder,
                Duration.ofDays(30));
    }

    @Test
    void reuseOfRotatedTokenRevokesSessionFamily() {
        UUID sessionId = UUID.randomUUID();
        RefreshToken rotated = new RefreshToken(UUID.randomUUID(), sessionId,
                "hash", null, Instant.now().plusSeconds(3600));
        rotated.setStatus(RefreshToken.Status.ROTATED);   // already used
        when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.of(rotated));

        assertThatThrownBy(() -> service.refresh("any-secret"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reuse");

        verify(refreshTokens).revokeSession(sessionId);   // whole family killed
    }
}
