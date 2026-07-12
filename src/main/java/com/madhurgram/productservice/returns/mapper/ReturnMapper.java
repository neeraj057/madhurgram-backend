package com.madhurgram.productservice.returns.mapper;

import com.madhurgram.productservice.returns.dto.ReturnRequestDTO;
import com.madhurgram.productservice.returns.entity.ReturnRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper component for bidirectional mapping between customer return requests and DTOs.
 */
@Component
public class ReturnMapper {

    /**
     * Converts a ReturnRequest entity to a ReturnRequestDTO.
     *
     * @param request the return request database entity
     * @return the mapped return request DTO
     */
    public ReturnRequestDTO toDTO(ReturnRequest request) {
        if (request == null) {
            return null;
        }
        return ReturnRequestDTO.builder()
                .id(request.getId())
                .orderId(request.getOrderId())
                .customerPhone(request.getCustomerPhone())
                .reason(request.getReason())
                .status(request.getStatus())
                .refundTransactionId(request.getRefundTransactionId())
                .createdAt(request.getCreatedAt())
                .approvedAt(request.getApprovedAt())
                .build();
    }

    /**
     * Converts a ReturnRequestDTO to a ReturnRequest entity.
     *
     * @param dto the return request DTO
     * @return the return request database entity
     */
    public ReturnRequest toEntity(ReturnRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return ReturnRequest.builder()
                .id(dto.getId())
                .orderId(dto.getOrderId())
                .customerPhone(dto.getCustomerPhone())
                .reason(dto.getReason())
                .status(dto.getStatus())
                .refundTransactionId(dto.getRefundTransactionId())
                .createdAt(dto.getCreatedAt())
                .approvedAt(dto.getApprovedAt())
                .build();
    }
}
