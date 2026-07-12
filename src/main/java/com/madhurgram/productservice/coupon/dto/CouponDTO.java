package com.madhurgram.productservice.coupon.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private Long id;
    private String code;
    private BigDecimal discountPercentage;
    private BigDecimal minOrderValue;
    private Boolean isActive;
    private int maxUsagePerUser;
}
