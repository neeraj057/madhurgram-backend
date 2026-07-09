package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.entity.AbandonedCart;
import com.madhurgram.productservice.cart.service.AbandonedCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/abandoned-carts")
@CrossOrigin(origins = "*")
public class AdminAbandonedCartController {

    private static final Logger log = LoggerFactory.getLogger(AdminAbandonedCartController.class);
    private final AbandonedCartService service;
    private final com.madhurgram.productservice.audit.service.AuditLogService auditLogService;

    public AdminAbandonedCartController(AbandonedCartService service, com.madhurgram.productservice.audit.service.AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AbandonedCartResponse>> getAbandonedCarts(
            @RequestParam(defaultValue = "30") int minutesAgo) {
        log.info("Received request to fetch admin abandoned carts older than {} minutes", minutesAgo);
        
        List<AbandonedCart> carts = service.getAbandonedCarts(minutesAgo);
        
        List<AbandonedCartResponse> responses = carts.stream()
                .map(c -> AbandonedCartResponse.builder()
                        .id(c.getId())
                        .phoneNumber(c.getPhoneNumber())
                        .customerName(c.getCustomerName())
                        .cartItemsJson(c.getCartItemsJson())
                        .totalAmount(c.getTotalAmount())
                        .lastUpdated(c.getLastUpdated())
                        .isRecovered(c.isRecovered())
                        .build())
                .toList();
                
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @org.springframework.cache.annotation.CacheEvict(value = "analytics", allEntries = true)
    public ResponseEntity<?> deleteAbandonedCart(@PathVariable Long id) {
        log.info("Received admin request to manually delete abandoned cart ID: {}", id);
        service.deleteAbandonedCart(id);
        auditLogService.log("DELETE_ABANDONED_CART", String.valueOf(id), "Admin manually deleted abandoned cart session");
        return ResponseEntity.ok(java.util.Map.of("message", "Abandoned cart deleted successfully."));
    }
}
