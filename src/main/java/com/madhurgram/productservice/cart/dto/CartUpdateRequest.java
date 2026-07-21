package com.madhurgram.productservice.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    private String phoneNumber;
    
    private String customerName;
    
    @NotBlank(message = "Cart items JSON cannot be empty")
    private String cartItemsJson;
    
    @NotNull(message = "Total amount is mandatory")
    @PositiveOrZero(message = "Total amount cannot be negative")
    private BigDecimal totalAmount;
}
