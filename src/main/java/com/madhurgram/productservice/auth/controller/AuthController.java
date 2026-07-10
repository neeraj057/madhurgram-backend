package com.madhurgram.productservice.auth.controller;

import com.madhurgram.productservice.auth.dto.AuthResponse;
import com.madhurgram.productservice.auth.dto.LoginRequest;
import com.madhurgram.productservice.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final JwtUtil jwtUtil;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/admin/login")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 5, windowSeconds = 60)
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest request) {
        log.info("Received login attempt for username: {}", request.getUsername());
        if (adminUsername.equals(request.getUsername()) && adminPassword.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername(), "ROLE_SUPER_ADMIN");
            log.info("Login successful for Super Admin: {}", request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
        } else if ("support".equals(request.getUsername()) && "Support@MadhurGram2026".equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername(), "ROLE_SUPPORT_STAFF");
            log.info("Login successful for Support Staff: {}", request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
        } else {
            log.warn("Login failed: Invalid credentials for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Username or Password");
        }
    }
}