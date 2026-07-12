package com.madhurgram.productservice.coupon.service.impl;

import com.madhurgram.productservice.coupon.dto.CouponDTO;
import com.madhurgram.productservice.coupon.entity.Coupon;
import com.madhurgram.productservice.coupon.entity.CouponUsage;
import com.madhurgram.productservice.coupon.mapper.CouponMapper;
import com.madhurgram.productservice.coupon.repository.CouponRepository;
import com.madhurgram.productservice.coupon.repository.CouponUsageRepository;
import com.madhurgram.productservice.coupon.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service implementation for managing customer discount coupons, 
 * checkout validations, and usage tracking logs.
 */
@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CouponMapper couponMapper;

    /**
     * Constructor injection for CouponServiceImpl.
     *
     * @param couponRepository      coupon database repository
     * @param couponUsageRepository coupon usage logs repository
     * @param couponMapper          coupon mapper instance
     */
    public CouponServiceImpl(CouponRepository couponRepository, 
                             CouponUsageRepository couponUsageRepository,
                             CouponMapper couponMapper) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.couponMapper = couponMapper;
    }

    /**
     * Validates if a coupon can be applied to checkout based on active status, min order threshold, and user limits.
     *
     * @param code       coupon coupon code
     * @param phone      buyer phone number verification
     * @param cartAmount cart total purchase amount
     * @return validated coupon details DTO
     */
    @Override
    @Transactional(readOnly = true)
    public CouponDTO validateCoupon(String code, String phone, BigDecimal cartAmount) {
        log.info("Validating coupon code: '{}' for phone: '{}', cartAmount: {}", code, phone, cartAmount);
        
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

        log.info("Coupon code '{}' is valid. Discount: {}%", coupon.getCode(), coupon.getDiscountPercentage());
        return couponMapper.toDTO(coupon);
    }

    /**
     * Logs the usage details of a coupon code upon successful checkout completion.
     *
     * @param code    coupon code used
     * @param phone   buyer phone number
     * @param orderId order index ID
     */
    @Override
    @Transactional
    public void recordCouponUsage(String code, String phone, Long orderId) {
        log.info("Recording coupon usage: code='{}', phone='{}', orderId={}", code, phone, orderId);
        
        if (code == null || code.trim().isEmpty() || phone == null || phone.trim().isEmpty() || orderId == null) {
            throw new IllegalArgumentException("Coupon code, phone number, and order ID are required parameters.");
        }

        CouponUsage usage = CouponUsage.builder()
                .couponCode(code.trim().toUpperCase())
                .customerPhone(phone.trim())
                .orderId(orderId)
                .usedAt(LocalDateTime.now())
                .build();
        couponUsageRepository.save(usage);
        log.info("Coupon usage recorded successfully.");
    }

    /**
     * Lists all coupon configurations.
     *
     * @return list of coupons DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<CouponDTO> getAllCoupons() {
        log.info("Admin request: fetch all coupon configurations");
        List<Coupon> coupons = couponRepository.findAll();
        return coupons.stream()
                .map(couponMapper::toDTO)
                .toList();
    }

    /**
     * Creates a new coupon configuration ruleset.
     *
     * @param dto coupon details payload DTO
     * @return created coupon details DTO
     */
    @Override
    @Transactional
    public CouponDTO createCoupon(CouponDTO dto) {
        log.info("Creating new coupon code: '{}'", dto.getCode());
        
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code cannot be empty.");
        }
        dto.setCode(dto.getCode().trim().toUpperCase());
        
        if (couponRepository.findByCodeIgnoreCase(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }
        
        if (dto.getDiscountPercentage() == null || dto.getDiscountPercentage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount percentage must be greater than 0.");
        }
        
        if (dto.getMinOrderValue() == null || dto.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum order value cannot be negative.");
        }

        Coupon coupon = couponMapper.toEntity(dto);
        Coupon saved = couponRepository.save(coupon);
        log.info("Successfully created coupon ID: {}", saved.getId());
        return couponMapper.toDTO(saved);
    }

    /**
     * Updates rules of an existing coupon configurations.
     *
     * @param id  target coupon ID
     * @param dto updated rules
     * @return updated coupon details DTO
     */
    @Override
    @Transactional
    public CouponDTO updateCoupon(Long id, CouponDTO dto) {
        log.info("Updating coupon ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Coupon ID must not be null.");
        }

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found with id: " + id));

        coupon.setIsActive(dto.getIsActive());
        coupon.setMinOrderValue(dto.getMinOrderValue());
        coupon.setDiscountPercentage(dto.getDiscountPercentage());
        coupon.setMaxUsagePerUser(dto.getMaxUsagePerUser());

        Coupon saved = couponRepository.save(coupon);
        log.info("Successfully updated coupon ID: {}", saved.getId());
        return couponMapper.toDTO(saved);
    }

    /**
     * Purges coupon ruleset configuration by ID.
     *
     * @param id target coupon ID
     */
    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        log.info("Deleting coupon ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Coupon ID must not be null.");
        }
        
        if (!couponRepository.existsById(id)) {
            throw new IllegalArgumentException("Coupon not found with id: " + id);
        }
        couponRepository.deleteById(id);
        log.info("Successfully deleted coupon ID: {}", id);
    }
}
