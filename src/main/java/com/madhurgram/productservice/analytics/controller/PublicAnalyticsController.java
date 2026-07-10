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
        double score = (double) System.currentTimeMillis();
        redisTemplate.opsForZSet().add("active_users_zset", clientId, score);
        return ResponseEntity.ok().build();
    }
}
