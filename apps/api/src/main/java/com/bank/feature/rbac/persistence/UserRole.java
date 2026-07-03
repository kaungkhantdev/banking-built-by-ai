package com.bank.feature.rbac.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/** Assignment of a {@link Role} to a user. */
@Entity
@Table(name = "user_roles",
        indexes = @Index(name = "ix_user_roles_user", columnList = "userId"))
public class UserRole {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private UUID userId;
    private UUID roleId;

    protected UserRole() {
    }

    public UserRole(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRoleId() {
        return roleId;
    }
}
