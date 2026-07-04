package com.madhurgram.productservice.customer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerStatsDTO(
    String name,
    String phoneNumber,
    int totalOrders,
    BigDecimal totalSpent,
    LocalDateTime lastOrderDate,
    boolean vip,
    String segment,
    String favoriteProduct,
    int favoriteProductQuantity
) {}
