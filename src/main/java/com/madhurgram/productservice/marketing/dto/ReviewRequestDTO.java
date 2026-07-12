package com.madhurgram.productservice.marketing.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDTO {
    private Long id;
    private Long orderId;
    private String customerName;
    private String customerPhone;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
}
