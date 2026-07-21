package com.madhurgram.productservice.auth.service;

import com.madhurgram.productservice.auth.entity.RefreshToken;
import com.madhurgram.productservice.auth.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling refresh token lifecycle:
 * creation, validation, rotation, revocation, and periodic cleanup.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshExpirationDays;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${jwt.refresh-expiration-days:7}") long refreshExpirationDays
    ) {
        this.repository = repository;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    /**
     * Creates and persists a new refresh token for the given user.
     *
     * @param username admin username
     * @param role     assigned role (e.g., ROLE_SUPER_ADMIN)
     * @return the generated opaque token string (UUID)
     */
    public String createRefreshToken(String username, String role) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .username(username)
                .role(role)
                .expiresAt(Instant.now().plus(Duration.ofDays(refreshExpirationDays)))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        repository.save(refreshToken);
        log.info("Created refresh token for user: '{}' (expires in {} days)", username, refreshExpirationDays);
        return refreshToken.getToken();
    }

    /**
     * Validates an existing refresh token and performs token rotation:
     * - Revokes the old token
     * - Creates a new token with the same user/role
     *
     * @param tokenString the refresh token UUID from the HttpOnly cookie
     * @return the new RefreshToken entity, or empty if validation fails
     */
    @Transactional
    public Optional<RefreshToken> validateAndRotate(String tokenString) {
        Optional<RefreshToken> existing = repository.findByTokenAndRevokedFalse(tokenString);

        if (existing.isEmpty()) {
            log.warn("Refresh token not found or already revoked: {}", maskToken(tokenString));
            return Optional.empty();
        }

        RefreshToken oldToken = existing.get();

        // Check expiry
        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired for user: '{}'", oldToken.getUsername());
            oldToken.setRevoked(true);
            repository.save(oldToken);
            return Optional.empty();
        }

        // Rotate: revoke old, create new
        oldToken.setRevoked(true);
        repository.save(oldToken);

        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .username(oldToken.getUsername())
                .role(oldToken.getRole())
                .expiresAt(Instant.now().plus(Duration.ofDays(refreshExpirationDays)))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        repository.save(newToken);
        log.info("Rotated refresh token for user: '{}'", oldToken.getUsername());
        return Optional.of(newToken);
    }

    /**
     * Revokes a specific refresh token (used during logout).
     *
     * @param tokenString the refresh token UUID
     * @return true if the token was found and revoked
     */
    @Transactional
    public boolean revokeToken(String tokenString) {
        Optional<RefreshToken> existing = repository.findByTokenAndRevokedFalse(tokenString);
        if (existing.isPresent()) {
            RefreshToken token = existing.get();
            token.setRevoked(true);
            repository.save(token);
            log.info("Revoked refresh token for user: '{}'", token.getUsername());
            return true;
        }
        log.warn("Attempted to revoke non-existent or already revoked token");
        return false;
    }

    /**
     * Revokes all active refresh tokens for a user (force logout from all sessions).
     *
     * @param username the admin username
     * @return number of tokens revoked
     */
    @Transactional
    public int revokeAllForUser(String username) {
        int count = repository.revokeAllByUsername(username);
        log.info("Revoked {} active refresh token(s) for user: '{}'", count, username);
        return count;
    }

    /**
     * Scheduled cleanup: removes expired tokens older than 1 day past their expiry.
     * Runs every 6 hours to keep the table lean.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // every 6 hours
    @Transactional
    public void cleanupExpiredTokens() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(1));
        int deleted = repository.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired refresh token(s)", deleted);
        }
    }

    /** Mask token for safe logging (show first 8 chars only) */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 8) + "***";
    }
}
