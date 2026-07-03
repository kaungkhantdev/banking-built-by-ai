package com.bank.feature.mfa.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "mfa_credentials")
public class MfaCredential extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private String secretEncrypted;

    @Column(nullable = false)
    private boolean confirmed;

    protected MfaCredential() {}

    public MfaCredential(UUID userId, String secretEncrypted) {
        this.userId = userId;
        this.secretEncrypted = secretEncrypted;
        this.confirmed = false;
    }

    public UUID getUserId() { return userId; }
    public String getSecretEncrypted() { return secretEncrypted; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
}
