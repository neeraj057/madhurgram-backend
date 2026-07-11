package com.madhurgram.productservice.coupon.repository;

import com.madhurgram.productservice.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCustomerPhoneAndCouponCodeIgnoreCase(String customerPhone, String couponCode);
}
