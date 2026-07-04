package com.bank.feature.auth.persistence;

import com.bank.shared.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

/**
 * A platform user (customer or operator). Authentication identity only — roles
 * and permissions live in the {@code rbac} feature; the banking relationship
 * lives in {@code accounts}.
 */
@Entity
@Table(name = "users")
public class User extends BaseAuditEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Consecutive failed logins since the last success (FR-14.4). Column added in V10. */
    @Column(name = "failed_attempt_count", nullable = false)
    private int failedLoginAttempts = 0;

    /** When set and in the future, authentication is temporarily blocked (column added in V10). */
    @Column
    private Instant lockedUntil;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = true;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    /** True while a temporary lockout is in effect. */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Record a failed login. Once {@code maxAttempts} is reached the account is
     * locked for {@code lockFor} and the counter resets.
     */
    public void recordFailedLogin(int maxAttempts, Duration lockFor) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plus(lockFor);
            this.failedLoginAttempts = 0;
        }
    }

    /** Clear all failed-login state (on success or operator unlock). */
    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
