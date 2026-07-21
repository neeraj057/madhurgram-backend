package com.madhurgram.productservice.coupon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private Long id;
    
    @NotBlank(message = "Coupon code cannot be empty")
    private String code;
    
    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.01", message = "Discount must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount cannot exceed 100%")
    private BigDecimal discountPercentage;
    
    @NotNull(message = "Minimum order value is required")
    @PositiveOrZero(message = "Minimum order value must be zero or positive")
    private BigDecimal minOrderValue;
    
    private Boolean isActive;
    
    @PositiveOrZero(message = "Max usage per user must be positive or zero")
    private int maxUsagePerUser;
}
