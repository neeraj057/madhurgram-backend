package com.madhurgram.productservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility helper class for generating, parsing, and validating JSON Web Tokens (JWT).
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_ROLE = "role";
    private static final String DEFAULT_ROLE = "ROLE_SUPER_ADMIN";

    private final String secretKey;
    private final long expirationMs;

    /**
     * Constructor injection for JwtUtil.
     *
     * @param secretKey    secret signing key configuration
     * @param expirationMs expiration duration milliseconds configuration
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs
    ) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret is required. Set jwt.secret or JWT_SECRET in environment variables.");
        }
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a default staff JWT token with Super Admin privileges.
     *
     * @param username staff username
     * @return signed JWT token
     */
    public String generateToken(String username) {
        return generateToken(username, DEFAULT_ROLE);
    }

    /**
     * Generates a signed staff JWT token containing role claims.
     *
     * @param username staff username
     * @param role     assigned authority role
     * @return signed JWT token
     */
    public String generateToken(String username, String role) {
        log.info("Generating secure JWT token for username: '{}' with role: '{}'", username, role);
        return Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_ROLE, role)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the assigned role claim from the token.
     *
     * @param token signed JWT token
     * @return the role claim value, or null if parsing fails
     */
    public String extractRole(String token) {
        try {
            return extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
        } catch (JwtException e) {
            log.warn("Failed to extract role from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the username (subject) from the token.
     *
     * @param token signed JWT token
     * @return username, or null if parsing fails
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (JwtException e) {
            log.warn("Failed to extract username from JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifies if the token belongs to the username and is not expired.
     *
     * @param token    signed JWT token
     * @param username target username to match
     * @return validation check result status
     */
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        boolean isValid = (extractedUsername != null && extractedUsername.equals(username) && !isTokenExpired(token));
        if (!isValid) {
            log.warn("Token validation failed for username: '{}'", username);
        }
        return isValid;
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        } catch (JwtException e) {
            log.warn("Token expiration validation check failed: {}", e.getMessage());
            return true;
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    /**
     * Parses an expired JWT token while still validating its signature.
     * Used by the token refresh flow where the token may have already expired
     * but we need to extract the username and role claims to issue a new token.
     *
     * @param token signed (possibly expired) JWT token
     * @return parsed Claims, or null if signature is invalid or token is malformed
     */
    public Claims extractClaimsIgnoringExpiry(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .setAllowedClockSkewSeconds(Long.MAX_VALUE / 1000)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("Failed to parse token (ignoring expiry): {}", e.getMessage());
            return null;
        }
    }
}