package com.madhurgram.productservice.coupon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "min_order_value", nullable = false, precision = 38, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "is_active", nullable = false)
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive = true;

    @Column(name = "max_usage_per_user", nullable = false)
    private int maxUsagePerUser = 1;
}
