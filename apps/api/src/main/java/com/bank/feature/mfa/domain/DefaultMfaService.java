package com.bank.feature.mfa.domain;

import com.bank.feature.mfa.persistence.*;
import com.bank.feature.mfa.web.dto.MfaEnrollView;
import com.bank.feature.mfa.web.dto.RecoveryCodesView;
import com.bank.shared.exception.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class DefaultMfaService implements MfaService {

    private static final String ISSUER = "BankCore";
    private static final int RECOVERY_CODE_COUNT = 10;

    private final MfaCredentialRepository credentials;
    private final RecoveryCodeRepository recoveryCodes;
    private final PasswordEncoder encoder;

    public DefaultMfaService(MfaCredentialRepository credentials,
                              RecoveryCodeRepository recoveryCodes,
                              PasswordEncoder encoder) {
        this.credentials = credentials;
        this.recoveryCodes = recoveryCodes;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public MfaEnrollView enroll(String userId) {
        UUID uid = UUID.fromString(userId);
        credentials.findByUserId(uid).ifPresent(c -> {
            if (c.isConfirmed()) throw new ApiException("MFA_ALREADY_ENROLLED", "MFA is already active", 409);
            credentials.delete(c);
        });

        String secret = generateBase32Secret();
        credentials.save(new MfaCredential(uid, secret)); // stored plaintext in stub; encrypt in prod
        String qrUri = "otpauth://totp/" + ISSUER + ":" + userId
                + "?secret=" + secret + "&issuer=" + ISSUER;
        return new MfaEnrollView(secret, qrUri, ISSUER);
    }

    @Override
    @Transactional
    public void confirmEnrollment(String userId, String totpCode) {
        UUID uid = UUID.fromString(userId);
        MfaCredential cred = requireCred(uid);
        if (cred.isConfirmed()) throw new ApiException("MFA_ALREADY_ENROLLED", "Already confirmed", 409);
        if (!validateTotp(cred.getSecretEncrypted(), totpCode)) {
            throw new ApiException("MFA_CODE_INVALID", "Invalid TOTP code", 422);
        }
        cred.setConfirmed(true);
        generateAndSaveRecoveryCodes(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verify(String userId, String totpCode) {
        UUID uid = UUID.fromString(userId);
        MfaCredential cred = requireCred(uid);
        if (!cred.isConfirmed()) throw new ApiException("MFA_NOT_ENROLLED", "MFA not enrolled", 422);
        return validateTotp(cred.getSecretEncrypted(), totpCode);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertStepUp(String userId, String totpCode) {
        UUID uid = UUID.fromString(userId);
        MfaCredential cred = credentials.findByUserId(uid).orElse(null);
        if (cred == null || !cred.isConfirmed()) {
            return;   // MFA not enrolled — nothing to step up
        }
        if (totpCode == null || !validateTotp(cred.getSecretEncrypted(), totpCode)) {
            throw new ApiException("MFA_REQUIRED",
                    "This operation requires a valid MFA code", 401);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecoveryCodesView getRecoveryCodes(String userId) {
        UUID uid = UUID.fromString(userId);
        long remaining = recoveryCodes.countByUserIdAndUsedFalse(uid);
        return new RecoveryCodesView(List.of(), remaining);
    }

    @Override
    @Transactional
    public RecoveryCodesView regenerateRecoveryCodes(String userId) {
        UUID uid = UUID.fromString(userId);
        recoveryCodes.deleteByUserId(uid);
        List<String> raw = generateAndSaveRecoveryCodes(uid);
        return new RecoveryCodesView(raw, raw.size());
    }

    @Override
    @Transactional
    public void disable(String userId) {
        UUID uid = UUID.fromString(userId);
        recoveryCodes.deleteByUserId(uid);
        credentials.findByUserId(uid).ifPresent(credentials::delete);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private List<String> generateAndSaveRecoveryCodes(UUID userId) {
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = generateRecoveryCode();
            raw.add(code);
            recoveryCodes.save(new RecoveryCode(userId, encoder.encode(code)));
        }
        return raw;
    }

    private MfaCredential requireCred(UUID userId) {
        return credentials.findByUserId(userId)
                .orElseThrow(() -> new ApiException("MFA_NOT_ENROLLED", "MFA not enrolled", 422));
    }

    /** RFC 6238 TOTP with SHA-1, 30-second window, ±1 step tolerance. */
    private boolean validateTotp(String base32Secret, String code) {
        if (code == null || code.length() != 6) return false;
        try {
            byte[] key = base32Decode(base32Secret);
            long epoch = Instant.now().getEpochSecond() / 30;
            for (long step = epoch - 1; step <= epoch + 1; step++) {
                if (hotp(key, step).equals(code)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String hotp(byte[] key, long counter) throws Exception {
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] h = mac.doFinal(msg);
        int offset = h[h.length - 1] & 0x0F;
        int code = ((h[offset] & 0x7F) << 24)
                | ((h[offset + 1] & 0xFF) << 16)
                | ((h[offset + 2] & 0xFF) << 8)
                | (h[offset + 3] & 0xFF);
        return String.format("%06d", code % 1_000_000);
    }

    private String generateBase32Secret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        // Base32 encoding (simplified: use Base64 and strip padding chars for stub)
        return Base64.getEncoder().withoutPadding().encodeToString(bytes)
                .toUpperCase().replace('+', 'A').replace('/', 'B');
    }

    private byte[] base32Decode(String secret) {
        // Reverse the simple encoding above
        String s = secret.toLowerCase().replace('a', '+').replace('b', '/');
        // pad to multiple of 4
        int pad = (4 - s.length() % 4) % 4;
        s += "=".repeat(pad);
        return Base64.getDecoder().decode(s);
    }

    private String generateRecoveryCode() {
        SecureRandom rng = new SecureRandom();
        return String.format("%04d-%04d-%04d", rng.nextInt(10000), rng.nextInt(10000), rng.nextInt(10000));
    }
}
