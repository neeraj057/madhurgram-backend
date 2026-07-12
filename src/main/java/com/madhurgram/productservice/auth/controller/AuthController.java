package com.madhurgram.productservice.auth.controller;

import com.madhurgram.productservice.auth.dto.AuthResponse;
import com.madhurgram.productservice.auth.dto.LoginRequest;
import com.madhurgram.productservice.security.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;

/**
 * Controller managing administrative authentication, login validation, and JWT token issuance.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for administrator authentication and JWT token generation")
public class AuthController {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_SUPPORT_STAFF = "ROLE_SUPPORT_STAFF";

    private static final String LOGIN_SUCCESS_MSG = "Login Successful";
    private static final String INVALID_CREDENTIALS_MSG = "Invalid Username or Password";

    private final JwtUtil jwtUtil;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${support.username}")
    private String supportUsername;

    @Value("${support.password}")
    private String supportPassword;

    /**
     * Constructor injection for AuthController.
     *
     * @param jwtUtil token utility for token construction
     */
    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates administrator credentials and issues a stateless JWT.
     * Rate limited to prevent brute force attacks.
     *
     * @param request login payload containing username and password
     * @return dynamic response detailing generated JWT token or unauthorized status message
     */
    @PostMapping("/admin/login")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 5, windowSeconds = 60)
    @Operation(summary = "Admin login authentication", description = "Verifies manager credentials and returns a secure JWT. Subject to brute force rate limiting.")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest request) {
        log.info("Request: administrator login attempt for username: '{}'", request.getUsername());
        
        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty()) {
            log.warn("Login rejected: username or password parameter is blank");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(INVALID_CREDENTIALS_MSG);
        }

        String usernameInput = request.getUsername().trim();
        String passwordInput = request.getPassword();

        // Constant-time comparison check to prevent timing attacks
        boolean isSuperAdmin = adminUsername.equals(usernameInput) && 
                safeStringEquals(adminPassword, passwordInput);
        
        boolean isSupportStaff = supportUsername.equals(usernameInput) && 
                safeStringEquals(supportPassword, passwordInput);

        if (isSuperAdmin) {
            String token = jwtUtil.generateToken(usernameInput, ROLE_SUPER_ADMIN);
            log.info("Login successful for Super Admin: '{}'", usernameInput);
            return ResponseEntity.ok(new AuthResponse(token, LOGIN_SUCCESS_MSG));
            
        } else if (isSupportStaff) {
            String token = jwtUtil.generateToken(usernameInput, ROLE_SUPPORT_STAFF);
            log.info("Login successful for Support Staff: '{}'", usernameInput);
            return ResponseEntity.ok(new AuthResponse(token, LOGIN_SUCCESS_MSG));
            
        } else {
            log.warn("Login failure: invalid credentials for username: '{}'", usernameInput);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(INVALID_CREDENTIALS_MSG);
        }
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