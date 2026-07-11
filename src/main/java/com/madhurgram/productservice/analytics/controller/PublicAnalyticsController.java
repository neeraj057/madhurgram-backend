package com.madhurgram.productservice.analytics.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller for public analytics tracking like shopper real-time active
 * connections / heartbeats.
 */
@Slf4j
@RestController
@RequestMapping("/api/public/analytics")
@Tag(name = "Public Analytics", description = "Public activity tracking endpoints for storefront sessions")
public class PublicAnalyticsController {

    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection for PublicAnalyticsController.
     *
     * @param redisTemplate string-based redis template client
     */
    public PublicAnalyticsController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Logs client connection heartbeat to track online concurrent shopper traffic.
     * Keeps user activity timestamp scores refreshed in Redis sorted set.
     *
     * @param clientId unique UUID key of client browser session
     * @param request  incoming HTTP servlet servlet info request
     * @return dynamic blank response status
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "Register user activity heartbeat", description = "Pushes activity heartbeat signal for client session to compute concurrent storefront usage.")
    public ResponseEntity<?> registerHeartbeat(
            @RequestParam String clientId,
            HttpServletRequest request) {
        log.info("Public request: heartbeat from client ID: '{}'", clientId);
        double score = (double) System.currentTimeMillis();

        redisTemplate.opsForZSet().add("active_users_zset", clientId, score);

        log.debug("Heartbeat registered in Redis for client ID: '{}'", clientId);
        return ResponseEntity.ok().build();
    }
}
