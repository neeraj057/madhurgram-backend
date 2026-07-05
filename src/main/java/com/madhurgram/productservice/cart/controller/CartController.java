package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.dto.CartUpdateRequest;
import com.madhurgram.productservice.cart.entity.AbandonedCart;
import com.madhurgram.productservice.cart.service.AbandonedCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private final AbandonedCartService service;

    public CartController(AbandonedCartService service) {
        this.service = service;
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateCart(@RequestBody CartUpdateRequest request) {
        log.info("Received request to sync/update cart for phone: {}", request.getPhoneNumber());
        try {
            AbandonedCart updated = service.updateCart(request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cart update payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating cart session: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to sync cart state.");
        }
    }

    @GetMapping("/recover")
    public ResponseEntity<?> recoverCart(@RequestParam String phone) {
        log.info("Received request to recover cart for phone: {}", phone);
        if (phone == null || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Phone number is required for recovery.");
        }

        return service.getCartToRecover(phone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("No active cart session available to recover for phone: {}", phone);
                    return ResponseEntity.notFound().build();
                });
    }
}
