package com.madhurgram.productservice.coupon.controller;

import com.madhurgram.productservice.coupon.entity.Coupon;
import com.madhurgram.productservice.coupon.service.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller for validating public discount coupons and administering coupons configurations.
 */
@Slf4j
@Validated
@RestController
@Tag(name = "Coupons", description = "Endpoints for customer coupon validation and admin management")
public class CouponController {

    private final CouponService couponService;

    /**
     * Constructor injection for CouponController.
     *
     * @param couponService coupon management service
     */
    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * Validates if a coupon code can be applied to a shopping cart.
     * Accessible by unauthenticated public checkout clients.
     *
     * @param code   the coupon code to check
     * @param phone  optional customer phone number targeting specific accounts
     * @param amount target cart order total amount to qualify limits
     * @return coupon details if valid
     */
    @GetMapping("/api/coupons/validate")
    @Operation(summary = "Validate checkout coupon", description = "Validates coupon eligibility based on purchase amount and optional buyer phone verification.")
    public ResponseEntity<?> validateCoupon(
            @RequestParam("code") String code,
            @RequestParam(value = "phone", required = false)
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @RequestParam("amount") BigDecimal amount) {
        log.info("Validate coupon: code='{}', phone='{}', amount={}", code, phone, amount);
        try {
            Coupon coupon = couponService.validateCoupon(code, phone, amount);
            log.info("Coupon '{}' is valid for checkout", code);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            log.warn("Coupon validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Retrieves all coupon configurations.
     *
     * @return list of coupons
     */
    @GetMapping("/api/admin/coupons")
    @Operation(summary = "List all coupons (Admin)", description = "Retrieves all coupon configurations for administration catalog views.")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        log.info("Admin request: list all coupons");
        List<Coupon> coupons = couponService.getAllCoupons();
        log.info("Returning {} coupon(s) to admin", coupons.size());
        return ResponseEntity.ok(coupons);
    }

    /**
     * Creates a new coupon configuration.
     *
     * @param coupon the coupon payload to create
     * @return the created coupon
     */
    @PostMapping("/api/admin/coupons")
    @Operation(summary = "Create coupon (Admin)", description = "Creates a new discount coupon configuration ruleset.")
    public ResponseEntity<?> createCoupon(@RequestBody Coupon coupon) {
        log.info("Admin request: create coupon code='{}'", coupon.getCode());
        try {
            Coupon created = couponService.createCoupon(coupon);
            log.info("Coupon '{}' successfully created with ID: {}", created.getCode(), created.getId());
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            log.warn("Coupon creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Updates an existing coupon configuration.
     *
     * @param id     ID of the coupon to update
     * @param coupon coupon details payload to apply
     * @return the updated coupon details
     */
    @PutMapping("/api/admin/coupons/{id}")
    @Operation(summary = "Update coupon (Admin)", description = "Updates details/rulesets of an existing discount coupon by ID.")
    public ResponseEntity<?> updateCoupon(@PathVariable("id") Long id, @RequestBody Coupon coupon) {
        log.info("Admin request: update coupon ID: {}", id);
        try {
            Coupon updated = couponService.updateCoupon(id, coupon);
            log.info("Coupon ID: {} successfully updated", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Coupon update failed for ID: {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Deletes a coupon configuration.
     *
     * @param id ID of the coupon to purge
     * @return status message
     */
    @DeleteMapping("/api/admin/coupons/{id}")
    @Operation(summary = "Delete coupon (Admin)", description = "Deletes an active coupon configurations ruleset by ID.")
    public ResponseEntity<?> deleteCoupon(@PathVariable("id") Long id) {
        log.info("Admin request: delete coupon ID: {}", id);
        try {
            couponService.deleteCoupon(id);
            log.info("Coupon ID: {} successfully deleted", id);
            return ResponseEntity.ok(Map.of("message", "Coupon deleted successfully."));
        } catch (IllegalArgumentException e) {
            log.warn("Coupon deletion failed for ID: {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
