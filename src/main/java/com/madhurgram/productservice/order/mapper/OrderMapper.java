package com.madhurgram.productservice.order.mapper;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component for translating order data models and line items into frontend DTOs.
 */
@Component
public class OrderMapper {

    /**
     * Maps an Order entity to an OrderResponseDTO.
     *
     * @param order the database order entity
     * @return the mapped order response DTO
     */
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }
        
        List<OrderResponseDTO.ItemDTO> items = Collections.emptyList();
        if (order.getOrderItems() != null) {
            items = order.getOrderItems().stream()
                    .map(this::toItemDTO)
                    .collect(Collectors.toList());
        }

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .pincode(order.getPincode())
                .cityState(order.getCityState())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .trackingNumber(order.getTrackingNumber())
                .courierName(order.getCourierName())
                .paymentStatus(order.getPaymentStatus())
                .paymentTransactionId(order.getPaymentTransactionId())
                .latitude(order.getLatitude())
                .longitude(order.getLongitude())
                .taxableAmount(order.getTaxableAmount())
                .cgstTotal(order.getCgstTotal())
                .sgstTotal(order.getSgstTotal())
                .igstTotal(order.getIgstTotal())
                .couponCode(order.getCouponCode())
                .discountAmount(order.getDiscountAmount())
                .orderItems(items)
                .build();
    }

    /**
     * Maps an OrderItem entity to an ItemDTO.
     *
     * @param item the order line item entity
     * @return the mapped line item DTO
     */
    public OrderResponseDTO.ItemDTO toItemDTO(OrderItem item) {
        if (item == null) {
            return null;
        }
        return OrderResponseDTO.ItemDTO.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .hsnCode(item.getHsnCode())
                .gstRate(item.getGstRate())
                .taxableAmount(item.getTaxableAmount())
                .cgstAmount(item.getCgstAmount())
                .sgstAmount(item.getSgstAmount())
                .igstAmount(item.getIgstAmount())
                .build();
    }
}
