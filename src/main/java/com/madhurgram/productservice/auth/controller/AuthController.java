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
        
        if (adminUsername.equals(request.getUsername()) && adminPassword.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername(), ROLE_SUPER_ADMIN);
            log.info("Login successful for Super Admin: '{}'", request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
            
        } else if (supportUsername.equals(request.getUsername()) && supportPassword.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername(), ROLE_SUPPORT_STAFF);
            log.info("Login successful for Support Staff: '{}'", request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
            
        } else {
            log.warn("Login failure: invalid credentials for username: '{}'", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Username or Password");
        }
    }
}