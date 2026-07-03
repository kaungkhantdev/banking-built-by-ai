package com.bank.feature.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * A single-use refresh token, stored as a SHA-256 <b>hash</b> of the secret —
 * never the secret itself. Tokens form a chain ({@code previousId}) within a
 * {@code sessionId} family. Re-use of an already-rotated token revokes the whole
 * family (theft detection).
 *
 * <p>Append-mostly with status flips; does not extend {@code BaseAuditEntity}
 * because its lifecycle (rotate/revoke) is modeled explicitly.
 */
@Entity
@Table(name = "refresh_tokens",
        indexes = @Index(name = "ix_refresh_hash", columnList = "tokenHash", unique = true))
public class RefreshToken {

    public enum Status { ACTIVE, ROTATED, REVOKED }

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    private UUID previousId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(UUID userId, UUID sessionId, String tokenHash,
                        UUID previousId, Instant expiresAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.previousId = previousId;
        this.status = Status.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
