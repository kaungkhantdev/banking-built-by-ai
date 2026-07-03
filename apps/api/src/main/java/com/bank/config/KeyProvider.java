package com.bank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Supplies the RSA keypair used to sign (private) and verify (public) access
 * tokens. RS256 (asymmetric) lets the auth module hold the private key while any
 * other module — or a future separate service — verifies with the public key, so
 * there is no shared secret to leak.
 *
 * <p>In production the keys are loaded from configured PEM material / a secrets
 * manager. When none is configured (local dev, tests) an ephemeral in-memory
 * keypair is generated at startup so the app runs without external setup.
 */
@Component
public class KeyProvider {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public KeyProvider(@Value("${security.jwt.private-key-pem:}") String privatePem,
                       @Value("${security.jwt.public-key-pem:}") String publicPem) {
        if (privatePem.isBlank() || publicPem.isBlank()) {
            KeyPair kp = generate();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            this.publicKey = (RSAPublicKey) kp.getPublic();
        } else {
            this.privateKey = Pem.readPrivate(privatePem);
            this.publicKey = Pem.readPublic(publicPem);
        }
    }

    public RSAPrivateKey privateKey() {
        return privateKey;
    }

    public RSAPublicKey publicKey() {
        return publicKey;
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot generate RSA keypair", e);
        }
    }
}
