package com.madhurgram.productservice.marketing.mapper;

import com.madhurgram.productservice.marketing.dto.ReviewRequestDTO;
import com.madhurgram.productservice.marketing.entity.ReviewRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper component for bidirectional conversion of auto-scheduler review requests and DTOs.
 */
@Component
public class ReviewMapper {

    /**
     * Converts a ReviewRequest entity to a ReviewRequestDTO.
     *
     * @param request the review request database entity
     * @return the review request DTO
     */
    public ReviewRequestDTO toDTO(ReviewRequest request) {
        if (request == null) {
            return null;
        }
        return ReviewRequestDTO.builder()
                .id(request.getId())
                .orderId(request.getOrderId())
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .status(request.getStatus())
                .scheduledAt(request.getScheduledAt())
                .sentAt(request.getSentAt())
                .build();
    }

    /**
     * Converts a ReviewRequestDTO to a ReviewRequest entity.
     *
     * @param dto the review request DTO
     * @return the review request database entity
     */
    public ReviewRequest toEntity(ReviewRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return ReviewRequest.builder()
                .id(dto.getId())
                .orderId(dto.getOrderId())
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .status(dto.getStatus())
                .scheduledAt(dto.getScheduledAt())
                .sentAt(dto.getSentAt())
                .build();
    }
}
