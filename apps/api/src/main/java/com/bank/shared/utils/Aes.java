package com.bank.shared.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM authenticated encryption for secrets at rest (webhook secrets,
 * stored file bytes). The key is supplied via {@code security.encryption.key}
 * (Base64, 32 bytes); a dev default is used when unset.
 */
@Component
public class Aes {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKeySpec key;

    public Aes(@Value("${security.encryption.key:ZGV2LWtleS0zMi1ieXRlcy1sb25nLWFlczI1Ni1nY20h}") String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        // Normalise to exactly 32 bytes for AES-256.
        byte[] k = new byte[32];
        System.arraycopy(raw, 0, k, 0, Math.min(raw.length, 32));
        this.key = new SecretKeySpec(k, "AES");
    }

    /** Encrypt UTF-8 plaintext; returns Base64(iv || ciphertext || tag). */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Reverse of {@link #encrypt}. */
    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[all.length - IV_LEN];
            ByteBuffer.wrap(all).get(iv).get(ct);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
