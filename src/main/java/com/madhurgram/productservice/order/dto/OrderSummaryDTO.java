package com.madhurgram.productservice.order.dto;

import com.madhurgram.productservice.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryDTO(
    Long orderId, 
    LocalDateTime orderDate, 
    BigDecimal totalAmount, 
    OrderStatus status // पक्का चेक करो कि यही टाइप है
) {}