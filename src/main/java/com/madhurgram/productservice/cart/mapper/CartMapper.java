package com.madhurgram.productservice.cart.mapper;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.entity.AbandonedCart;
import org.springframework.stereotype.Component;

/**
 * Component responsible for mapping between cart entities and DTO response structures.
 */
@Component
public class CartMapper {

    /**
     * Converts an AbandonedCart entity to an AbandonedCartResponse DTO.
     *
     * @param cart the source entity
     * @return the mapped response DTO
     */
    public AbandonedCartResponse toResponse(AbandonedCart cart) {
        if (cart == null) {
            return null;
        }
        return AbandonedCartResponse.builder()
                .id(cart.getId())
                .phoneNumber(cart.getPhoneNumber())
                .customerName(cart.getCustomerName())
                .cartItemsJson(cart.getCartItemsJson())
                .totalAmount(cart.getTotalAmount())
                .lastUpdated(cart.getLastUpdated())
                .isRecovered(cart.isRecovered())
                .reminderSent(cart.isReminderSent())
                .reminderSentAt(cart.getReminderSentAt())
                .build();
    }
}
