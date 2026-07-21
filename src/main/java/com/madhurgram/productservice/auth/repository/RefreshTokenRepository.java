package com.madhurgram.productservice.auth.repository;

import com.madhurgram.productservice.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data repository for refresh token persistence.
 * Supports lookup, revocation, and cleanup operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Find a valid (non-revoked) refresh token by its UUID string */
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /** Revoke all active tokens for a given username (force logout everywhere) */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.username = :username AND rt.revoked = false")
    int revokeAllByUsername(String username);

    /** Delete tokens that are both expired and revoked (cleanup) */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredBefore(Instant cutoff);
}
