package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.dto.CartUpdateRequest;
import com.madhurgram.productservice.cart.service.AbandonedCartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

/**
 * Controller for managing shopping cart sessions and cart recovery.
 * 
 * <p>Persists abandoned carts and recovers active sessions using phone numbers.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Shopping cart sync and recovery endpoints")
public class CartController {

    private final AbandonedCartService service;

    /**
     * Constructor injection for CartController.
     *
     * @param service the service managing abandoned carts
     */
    public CartController(AbandonedCartService service) {
        this.service = service;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "******" + phone.substring(phone.length() - 4);
    }

    /**
     * Syncs or updates the current state of a shopping cart.
     * Rate limited to prevent session spam.
     *
     * @param request the cart update payload detailing items and total amount
     * @return the saved cart session
     */
    @PostMapping("/update")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 10, windowSeconds = 60)
    @Operation(summary = "Update cart state", description = "Updates items and amounts in the active cart state. Subject to rate limits.")
    public ResponseEntity<?> updateCart(@RequestBody CartUpdateRequest request) {
        log.info("Request: sync cart state for phone: '{}'", maskPhone(request.getPhoneNumber()));
        try {
            AbandonedCartResponse updated = service.updateCart(request);
            log.info("Cart state successfully updated for phone: '{}'", maskPhone(request.getPhoneNumber()));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid cart update format: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update cart for phone: '{}': {}", maskPhone(request.getPhoneNumber()), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to sync cart state.");
        }
    }

    /**
     * Recovers a saved cart session for checkout continuity.
     * Rate limited to prevent brute force.
     *
     * @param phone the customer's validated phone number
     * @return the saved cart session if present, otherwise 404
     */
    @GetMapping("/recover")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 10, windowSeconds = 60)
    @Operation(summary = "Recover cart session", description = "Retrieves active checkout recovery items by phone number. Subject to rate limits.")
    public ResponseEntity<?> recoverCart(
            @RequestParam
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone) {
        log.info("Request: recover cart for phone: '{}'", maskPhone(phone));

        return service.getCartToRecover(phone)
                .map(cart -> {
                    log.info("Cart successfully recovered for phone: '{}'", maskPhone(phone));
                    return ResponseEntity.ok(cart);
                })
                .orElseGet(() -> {
                    log.warn("No active cart found to recover for phone: '{}'", maskPhone(phone));
                    return ResponseEntity.notFound().build();
                });
    }
}
