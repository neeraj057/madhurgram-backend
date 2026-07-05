package com.madhurgram.productservice.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartUpdateRequest {
    private String phoneNumber;
    private String customerName;
    private String cartItemsJson;
    private BigDecimal totalAmount;
}
