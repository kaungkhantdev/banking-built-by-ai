package com.bank.feature.auth.domain;

import com.bank.feature.auth.persistence.*;
import com.bank.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultPasswordService implements PasswordService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPasswordService.class);
    private static final int HISTORY_DEPTH = 10;

    private final UserRepository users;
    private final PasswordHistoryRepository history;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;

    public DefaultPasswordService(UserRepository users,
                                   PasswordHistoryRepository history,
                                   PasswordResetTokenRepository resetTokens,
                                   PasswordEncoder encoder) {
        this.users = users;
        this.history = history;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "Unknown user", 404));

        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException("INVALID_CREDENTIALS", "Current password is incorrect", 422);
        }

        assertNotReused(UUID.fromString(userId), newPassword);

        String newHash = encoder.encode(newPassword);
        history.save(new PasswordHistory(user.getId(), user.getPasswordHash()));
        // Update the user's password — we need setPasswordHash on User
        // Using reflection-free approach: the User entity must expose setPasswordHash
        // We'll patch the entity field directly through the domain
        user.setPasswordHash(newHash);
    }

    @Override
    @Transactional
    public void initiateReset(String email) {
        users.findByEmail(email).ifPresent(user -> {
            String raw = generateToken();
            String hash = sha256(raw);
            resetTokens.save(new PasswordResetToken(
                    user.getId(), hash, Instant.now().plus(15, ChronoUnit.MINUTES)));
            // Stub: log the reset link instead of sending an email
            log.info("[PASSWORD RESET] token={} for user={}", raw, user.getId());
        });
        // Always return 200 to prevent email enumeration
    }

    @Override
    @Transactional
    public void confirmReset(String token, String newPassword) {
        String hash = sha256(token);
        PasswordResetToken prt = resetTokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException("RESET_TOKEN_INVALID", "Token not found or already used", 400));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("RESET_TOKEN_EXPIRED", "Token is expired or already used", 400);
        }

        assertNotReused(prt.getUserId(), newPassword);

        User user = users.findById(prt.getUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "Unknown user", 404));

        history.save(new PasswordHistory(user.getId(), user.getPasswordHash()));
        user.setPasswordHash(encoder.encode(newPassword));
        prt.setUsed(true);
    }

    private void assertNotReused(UUID userId, String newPassword) {
        List<PasswordHistory> recent = history.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        boolean reused = recent.stream().anyMatch(h -> encoder.matches(newPassword, h.getPasswordHash()));
        if (reused) {
            throw new ApiException("PASSWORD_REUSED",
                    "New password must differ from the last " + HISTORY_DEPTH + " passwords", 422);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
