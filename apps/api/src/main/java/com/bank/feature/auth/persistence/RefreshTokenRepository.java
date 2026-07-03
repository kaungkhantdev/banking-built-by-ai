package com.bank.feature.auth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoke every token in a session family — used on reuse detection. */
    @Modifying
    @Query("update RefreshToken t set t.status = com.bank.feature.auth.persistence.RefreshToken$Status.REVOKED "
            + "where t.sessionId = :sessionId")
    void revokeSession(@Param("sessionId") UUID sessionId);
}
