package com.madhurgram.productservice.coupon.controller;

import com.madhurgram.productservice.coupon.entity.Coupon;
import com.madhurgram.productservice.coupon.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
public class CouponController {

    private static final Logger log = LoggerFactory.getLogger(CouponController.class);
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // 🌍 Public endpoint: Check if coupon is valid for shopping cart checkout
    @GetMapping("/api/coupons/validate")
    public ResponseEntity<?> validateCoupon(
            @RequestParam("code") String code,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam("amount") BigDecimal amount) {
        log.info("Validate coupon request received: code={}, phone={}, amount={}", code, phone, amount);
        try {
            Coupon coupon = couponService.validateCoupon(code, phone, amount);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔐 Admin endpoints: Coupon list, create, update, delete
    @GetMapping("/api/admin/coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        log.info("Admin request: fetch all coupons");
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<?> createCoupon(@RequestBody Coupon coupon) {
        log.info("Admin request: create coupon: {}", coupon.getCode());
        try {
            Coupon created = couponService.createCoupon(coupon);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/api/admin/coupons/{id}")
    public ResponseEntity<?> updateCoupon(@PathVariable("id") Long id, @RequestBody Coupon coupon) {
        log.info("Admin request: update coupon ID: {}", id);
        try {
            Coupon updated = couponService.updateCoupon(id, coupon);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<?> deleteCoupon(@PathVariable("id") Long id) {
        log.info("Admin request: delete coupon ID: {}", id);
        try {
            couponService.deleteCoupon(id);
            return ResponseEntity.ok("Coupon deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
