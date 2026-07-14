package com.madhurgram.productservice.feedback.mapper;

import com.madhurgram.productservice.feedback.dto.CustomerFeedbackDTO;
import com.madhurgram.productservice.feedback.entity.CustomerFeedback;
import org.springframework.stereotype.Component;

/**
 * Mapper component for bidirectional mapping of shopper feedback forms and DTOs.
 */
@Component
public class FeedbackMapper {

    /**
     * Converts a CustomerFeedback entity to a CustomerFeedbackDTO.
     *
     * @param feedback the customer feedback entity
     * @return the mapped DTO
     */
    public CustomerFeedbackDTO toDTO(CustomerFeedback feedback) {
        if (feedback == null) {
            return null;
        }
        return CustomerFeedbackDTO.builder()
                .id(feedback.getId())
                .orderId(feedback.getOrderId())
                .customerName(feedback.getCustomerName())
                .sentiment(feedback.getSentiment())
                .rating(feedback.getRating())
                .feedbackText(feedback.getFeedbackText())
                .selectedChips(feedback.getSelectedChips())
                .productImageUrl(feedback.getProductImageUrl())
                .createdAt(feedback.getCreatedAt())
                .isApproved(feedback.getIsApproved() != null ? feedback.getIsApproved() : true)
                .build();
    }

    /**
     * Converts a CustomerFeedbackDTO to a CustomerFeedback entity.
     *
     * @param dto the feedback DTO
     * @return the feedback database entity
     */
    public CustomerFeedback toEntity(CustomerFeedbackDTO dto) {
        if (dto == null) {
            return null;
        }
        return CustomerFeedback.builder()
                .id(dto.getId())
                .orderId(dto.getOrderId())
                .customerName(dto.getCustomerName())
                .sentiment(dto.getSentiment())
                .rating(dto.getRating())
                .feedbackText(dto.getFeedbackText())
                .selectedChips(dto.getSelectedChips())
                .productImageUrl(dto.getProductImageUrl())
                .createdAt(dto.getCreatedAt())
                .isApproved(dto.getIsApproved() != null ? dto.getIsApproved() : true)
                .build();
    }
}
