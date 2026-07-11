package com.madhurgram.productservice.coupon.service.impl;

import com.madhurgram.productservice.coupon.entity.Coupon;
import com.madhurgram.productservice.coupon.entity.CouponUsage;
import com.madhurgram.productservice.coupon.repository.CouponRepository;
import com.madhurgram.productservice.coupon.repository.CouponUsageRepository;
import com.madhurgram.productservice.coupon.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    public CouponServiceImpl(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, String phone, BigDecimal cartAmount) {
        log.info("Validating coupon code: {} for phone: {}, cartAmount: {}", code, phone, cartAmount);
        
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code cannot be empty.");
        }
        
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid coupon code."));

        if (coupon.getIsActive() == null || !coupon.getIsActive()) {
            throw new IllegalArgumentException("This coupon is currently inactive.");
        }

        if (cartAmount == null || cartAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            BigDecimal shortage = coupon.getMinOrderValue().subtract(cartAmount != null ? cartAmount : BigDecimal.ZERO);
            throw new IllegalArgumentException("Add items worth ₹" + String.format("%.2f", shortage) + " more to apply this coupon.");
        }

        if (phone != null && !phone.trim().isEmpty()) {
            String sanitizedPhone = phone.trim();
            long usageCount = couponUsageRepository.countByCustomerPhoneAndCouponCodeIgnoreCase(sanitizedPhone, code.trim());
            if (usageCount >= coupon.getMaxUsagePerUser()) {
                throw new IllegalArgumentException("You have already redeemed this one-time coupon.");
            }
        }

        log.info("Coupon code {} is valid. Discount: {}%", coupon.getCode(), coupon.getDiscountPercentage());
        return coupon;
    }

    @Override
    @Transactional
    public void recordCouponUsage(String code, String phone, Long orderId) {
        log.info("Recording coupon usage: code={}, phone={}, orderId={}", code, phone, orderId);
        CouponUsage usage = CouponUsage.builder()
                .couponCode(code.trim().toUpperCase())
                .customerPhone(phone.trim())
                .orderId(orderId)
                .usedAt(LocalDateTime.now())
                .build();
        couponUsageRepository.save(usage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code cannot be empty.");
        }
        coupon.setCode(coupon.getCode().trim().toUpperCase());
        
        if (couponRepository.findByCodeIgnoreCase(coupon.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }
        
        if (coupon.getDiscountPercentage() == null || coupon.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount percentage must be greater than 0.");
        }
        
        if (coupon.getMinOrderValue() == null || coupon.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum order value cannot be negative.");
        }

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public Coupon updateCoupon(Long id, Coupon dto) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with id: " + id));

        coupon.setIsActive(dto.getIsActive());
        coupon.setMinOrderValue(dto.getMinOrderValue());
        coupon.setDiscountPercentage(dto.getDiscountPercentage());
        coupon.setMaxUsagePerUser(dto.getMaxUsagePerUser());

        return couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new IllegalArgumentException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
    }
}
