package com.bank.feature.mfa.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recovery_codes")
public class RecoveryCode {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;

    protected RecoveryCode() {}

    public RecoveryCode(UUID userId, String codeHash) {
        this.userId = userId;
        this.codeHash = codeHash;
        this.used = false;
        this.createdAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
