package com.madhurgram.productservice.returns.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestDTO {
    private Long id;
    private Long orderId;
    private String customerPhone;
    private String reason;
    private String status;
    private String refundTransactionId;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}
