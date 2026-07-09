package com.madhurgram.productservice.analytics.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/public/analytics")
@CrossOrigin(origins = "*")
public class PublicAnalyticsController {

    private final StringRedisTemplate redisTemplate;

    public PublicAnalyticsController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<?> registerHeartbeat(
            @RequestParam String clientId,
            HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        // Track the active user session in Redis with 30s expiration
        String key = "active_user_session:" + clientId;
        redisTemplate.opsForValue().set(key, clientIp, 30, TimeUnit.SECONDS);
        return ResponseEntity.ok().build();
    }
}
