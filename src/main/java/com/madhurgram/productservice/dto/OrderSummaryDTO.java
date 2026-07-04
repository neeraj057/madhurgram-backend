package com.madhurgram.productservice.dto;

import com.madhurgram.productservice.entity.OrderStatus; // तुम्हारा Enum
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryDTO(
    Long orderId, 
    LocalDateTime orderDate, 
    BigDecimal totalAmount, 
    OrderStatus status // पक्का चेक करो कि यही टाइप है
) {}