package com.madhurgram.productservice.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity representing a server-side refresh token.
 * Refresh tokens are stored in the database to support:
 * - Token revocation (logout, force-logout)
 * - Token rotation (each refresh invalidates the old token)
 * - Expiry tracking independent of JWT claims
 *
 * The table is auto-created by Hibernate ddl-auto=update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token", columnList = "token", unique = true),
        @Index(name = "idx_refresh_username", columnList = "username")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opaque UUID token string — stored in HttpOnly cookie */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /** Admin username this token belongs to */
    @Column(nullable = false, length = 100)
    private String username;

    /** Role claim to carry over when issuing new access tokens */
    @Column(nullable = false, length = 50)
    private String role;

    /** Absolute expiration timestamp */
    @Column(nullable = false)
    private Instant expiresAt;

    /** Revocation flag — set to true on logout or token rotation */
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    /** Creation timestamp for audit trail */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
