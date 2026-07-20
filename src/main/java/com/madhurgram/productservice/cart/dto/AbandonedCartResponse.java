package com.madhurgram.productservice.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbandonedCartResponse {
    private Long id;
    private String phoneNumber;
    private String customerName;
    @JsonRawValue
    private String cartItemsJson;
    private BigDecimal totalAmount;
    private LocalDateTime lastUpdated;
    private boolean isRecovered;
    private boolean reminderSent;
    private LocalDateTime reminderSentAt;
}
