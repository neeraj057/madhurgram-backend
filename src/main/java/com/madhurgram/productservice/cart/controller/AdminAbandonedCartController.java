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

    public AdminAbandonedCartController(AbandonedCartService service) {
        this.service = service;
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
}
