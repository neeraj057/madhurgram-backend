package com.madhurgram.productservice.coupon.service;

import com.madhurgram.productservice.coupon.entity.Coupon;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    Coupon validateCoupon(String code, String phone, BigDecimal cartAmount);
    void recordCouponUsage(String code, String phone, Long orderId);
    List<Coupon> getAllCoupons();
    Coupon createCoupon(Coupon coupon);
    Coupon updateCoupon(Long id, Coupon coupon);
    void deleteCoupon(Long id);
}
