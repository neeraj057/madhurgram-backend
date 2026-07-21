package com.madhurgram.productservice.auth.controller;

import com.madhurgram.productservice.auth.dto.AuthResponse;
import com.madhurgram.productservice.auth.dto.LoginRequest;
import com.madhurgram.productservice.auth.entity.RefreshToken;
import com.madhurgram.productservice.auth.service.RefreshTokenService;
import com.madhurgram.productservice.security.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;

/**
 * Controller managing administrative authentication with production-grade security:
 * - Access Token (short-lived JWT, 15 min) returned in response body
 * - Refresh Token (long-lived UUID, 7 days) set as HttpOnly Secure cookie
 * - Token rotation on every refresh
 * - Server-side revocation via database
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Admin authentication with Access + Refresh token architecture")
public class AuthController {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_SUPPORT_STAFF = "ROLE_SUPPORT_STAFF";
    private static final String REFRESH_COOKIE_NAME = "madhurgram_refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private static final String LOGIN_SUCCESS_MSG = "Login Successful";
    private static final String INVALID_CREDENTIALS_MSG = "Invalid Username or Password";

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${support.username}")
    private String supportUsername;

    @Value("${support.password}")
    private String supportPassword;

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @Value("${madhurgram.cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Authenticates admin credentials and issues a dual-token response:
     * - Access token (JWT, 15 min) in JSON response body
     * - Refresh token (UUID, 7 days) as HttpOnly Secure cookie
     *
     * Rate limited to prevent brute force attacks.
     */
    @PostMapping("/admin/login")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 5, windowSeconds = 60)
    @Operation(summary = "Admin login", description = "Verifies credentials and returns access token + sets refresh token cookie")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest request, HttpServletResponse response) {
        log.info("Request: administrator login attempt for username: '{}'", request.getUsername());

        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            log.warn("Login rejected: username or password parameter is blank");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALID_CREDENTIALS_MSG);
        }

        String usernameInput = request.getUsername().trim();
        String passwordInput = request.getPassword();

        // Constant-time comparison to prevent timing attacks
        boolean isSuperAdmin = adminUsername.equals(usernameInput) &&
                safeStringEquals(adminPassword, passwordInput);

        boolean isSupportStaff = supportUsername.equals(usernameInput) &&
                safeStringEquals(supportPassword, passwordInput);

        if (isSuperAdmin) {
            return issueTokens(usernameInput, ROLE_SUPER_ADMIN, response);
        } else if (isSupportStaff) {
            return issueTokens(usernameInput, ROLE_SUPPORT_STAFF, response);
        } else {
            log.warn("Login failure: invalid credentials for username: '{}'", usernameInput);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_CREDENTIALS_MSG);
        }
    }

    /**
     * Refreshes an expired session using the HttpOnly refresh token cookie.
     * Performs token rotation: old refresh token is revoked, new one is issued.
     *
     * The browser automatically sends the HttpOnly cookie — no Authorization header needed.
     */
    @PostMapping("/admin/refresh")
    @Operation(summary = "Refresh access token", description = "Uses HttpOnly refresh cookie to issue new access + refresh tokens with rotation")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenValue,
            HttpServletResponse response
    ) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            log.warn("Token refresh rejected: no refresh cookie present");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token");
        }

        Optional<RefreshToken> rotated = refreshTokenService.validateAndRotate(refreshTokenValue);

        if (rotated.isEmpty()) {
            // Clear the invalid/expired cookie
            clearRefreshCookie(response);
            log.warn("Token refresh rejected: invalid or expired refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        RefreshToken newRefreshToken = rotated.get();
        String accessToken = jwtUtil.generateToken(newRefreshToken.getUsername(), newRefreshToken.getRole());

        // Set new refresh token cookie
        setRefreshCookie(response, newRefreshToken.getToken());

        log.info("Token refreshed successfully for user: '{}' (rotation complete)", newRefreshToken.getUsername());
        return ResponseEntity.ok(new AuthResponse(accessToken, "Token refreshed"));
    }

    /**
     * Logs out the admin by revoking the refresh token and clearing the cookie.
     * After this, the current access token remains valid until its short expiry (15 min max),
     * but no new access tokens can be issued.
     */
    @PostMapping("/admin/logout")
    @Operation(summary = "Admin logout", description = "Revokes refresh token and clears HttpOnly cookie")
    public ResponseEntity<?> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshTokenValue,
            HttpServletResponse response
    ) {
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            refreshTokenService.revokeToken(refreshTokenValue);
        }

        clearRefreshCookie(response);
        log.info("Admin logged out successfully");
        return ResponseEntity.ok().body("{\"message\": \"Logged out successfully\"}");
    }

    // ─── Private Helpers ──────────────────────────────────────────────

    /**
     * Issues both access token (in body) and refresh token (in HttpOnly cookie).
     */
    private ResponseEntity<?> issueTokens(String username, String role, HttpServletResponse response) {
        // Generate short-lived access token (JWT)
        String accessToken = jwtUtil.generateToken(username, role);

        // Generate long-lived refresh token (UUID, stored in DB)
        String refreshToken = refreshTokenService.createRefreshToken(username, role);

        // Set refresh token as HttpOnly Secure cookie
        setRefreshCookie(response, refreshToken);

        log.info("Login successful for '{}' with role: '{}'", username, role);
        return ResponseEntity.ok(new AuthResponse(accessToken, LOGIN_SUCCESS_MSG));
    }

    /**
     * Sets the refresh token as an HttpOnly cookie with security attributes.
     */
    private void setRefreshCookie(HttpServletResponse response, String tokenValue) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokenValue)
                .httpOnly(true)                  // JavaScript cannot access this cookie
                .secure(cookieSecure)            // HTTPS only in production
                .sameSite("Strict")              // Prevent CSRF — same-site requests only
                .path(REFRESH_COOKIE_PATH)       // Only sent to /api/auth/* endpoints
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Clears the refresh token cookie on logout or failed refresh.
     */
    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)                      // Immediate expiry = delete
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Compares two strings in constant-time to prevent timing attacks.
     */
    private boolean safeStringEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(), b.getBytes());
    }
}