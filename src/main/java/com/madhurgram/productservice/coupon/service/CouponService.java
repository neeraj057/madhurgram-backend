package com.madhurgram.productservice.coupon.service;

import com.madhurgram.productservice.coupon.dto.CouponDTO;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    CouponDTO validateCoupon(String code, String phone, BigDecimal cartAmount);
    void recordCouponUsage(String code, String phone, Long orderId);
    List<CouponDTO> getAllCoupons();
    CouponDTO createCoupon(CouponDTO coupon);
    CouponDTO updateCoupon(Long id, CouponDTO coupon);
    void deleteCoupon(Long id);
}
