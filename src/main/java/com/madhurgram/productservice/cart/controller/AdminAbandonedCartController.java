package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.service.AbandonedCartService;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for administrators to list and delete inactive checkout cart sessions.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/abandoned-carts")
@Tag(name = "Admin — Abandoned Carts", description = "Endpoints for monitoring and purging abandoned buyer shopping carts")
public class AdminAbandonedCartController {

    private final AbandonedCartService service;
    private final AuditLogService auditLogService;

    /**
     * Constructor injection for AdminAbandonedCartController.
     *
     * @param service          abandoned cart service
     * @param auditLogService  audit logger service
     */
    public AdminAbandonedCartController(AbandonedCartService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves a list of shopping carts that were modified but not checked out.
     *
     * @param minutesAgo standard cutoff duration in minutes to identify idle sessions
     * @return a list of abandoned cart session metadata DTOs
     */
    @GetMapping
    @Operation(summary = "List abandoned carts", description = "Retrieves shopping carts that have been idle for at least the specified minutes limit.")
    public ResponseEntity<List<AbandonedCartResponse>> getAbandonedCarts(
            @RequestParam(defaultValue = "30") int minutesAgo) {
        log.info("Admin request: fetch abandoned carts older than {} minutes", minutesAgo);
        List<AbandonedCartResponse> responses = service.getAbandonedCarts(minutesAgo);
        log.info("Returning {} abandoned cart(s) to admin", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Deletes an abandoned cart entry by ID.
     * Clears dashboard/analytics caches.
     *
     * @param id the abandoned cart session ID
     * @return status message
     */
    @DeleteMapping("/{id}")
    @CacheEvict(value = "analytics", allEntries = true)
    @Operation(summary = "Purge abandoned cart session", description = "Manually deletes an inactive checkout session by ID and flushes analytic caches.")
    public ResponseEntity<?> deleteAbandonedCart(@PathVariable Long id) {
        log.info("Admin request: manually purge abandoned cart ID: {}", id);
        
        service.deleteAbandonedCart(id);
        auditLogService.log("DELETE_ABANDONED_CART", String.valueOf(id), "Admin manually deleted abandoned cart session");
        
        log.info("Abandoned cart ID: {} successfully deleted and cache evicted", id);
        return ResponseEntity.ok(Map.of("message", "Abandoned cart deleted successfully."));
    }
}
