package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.AuthResponse;
import com.madhurgram.productservice.dto.LoginRequest;
import com.madhurgram.productservice.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest request) {
        // 🔒 Hardcoded Admin Credentials (For MathurGram Admin Panel)
        String adminUser = "admin";
        String adminPass = "MadhurGram@2026";

        if (adminUser.equals(request.getUsername()) && adminPass.equals(request.getPassword())) {
            String token = jwtUtil.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login Successful"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Username or Password");
        }
    }
}