package com.madhurgram.productservice.order.dto;

import com.madhurgram.productservice.order.entity.OrderStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDTO {
    private Long id;
    private String customerName;
    private String phoneNumber;
    private String address;
    private String pincode;
    private String cityState;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private String trackingNumber;
    private String courierName;
    private String paymentStatus;
    private String paymentTransactionId;
    private Double latitude;
    private Double longitude;
    private BigDecimal taxableAmount;
    private BigDecimal cgstTotal;
    private BigDecimal sgstTotal;
    private BigDecimal igstTotal;
    private List<ItemDTO> orderItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemDTO {
        private Long id;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private String hsnCode;
        private BigDecimal gstRate;
        private BigDecimal taxableAmount;
        private BigDecimal cgstAmount;
        private BigDecimal sgstAmount;
        private BigDecimal igstAmount;
    }
}