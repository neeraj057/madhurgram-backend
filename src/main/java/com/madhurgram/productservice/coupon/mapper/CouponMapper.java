package com.madhurgram.productservice.coupon.mapper;

import com.madhurgram.productservice.coupon.dto.CouponDTO;
import com.madhurgram.productservice.coupon.entity.Coupon;
import org.springframework.stereotype.Component;

/**
 * Mapper component for bidirectional mapping between coupon entities and DTO wrappers.
 */
@Component
public class CouponMapper {

    /**
     * Converts a Coupon entity to a CouponDTO.
     *
     * @param coupon the database entity
     * @return the mapped coupon DTO
     */
    public CouponDTO toDTO(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return CouponDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountPercentage(coupon.getDiscountPercentage())
                .minOrderValue(coupon.getMinOrderValue())
                .isActive(coupon.getIsActive())
                .maxUsagePerUser(coupon.getMaxUsagePerUser())
                .build();
    }

    /**
     * Converts a CouponDTO to a Coupon entity.
     *
     * @param dto the coupon DTO
     * @return the mapped coupon database entity
     */
    public Coupon toEntity(CouponDTO dto) {
        if (dto == null) {
            return null;
        }
        return Coupon.builder()
                .id(dto.getId())
                .code(dto.getCode())
                .discountPercentage(dto.getDiscountPercentage())
                .minOrderValue(dto.getMinOrderValue())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .maxUsagePerUser(dto.getMaxUsagePerUser())
                .build();
    }
}
