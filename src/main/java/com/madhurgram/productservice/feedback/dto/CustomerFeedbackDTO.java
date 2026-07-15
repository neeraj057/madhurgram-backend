package com.madhurgram.productservice.feedback.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedbackDTO {
    private Long id;
    private Long orderId;
    private String customerName;
    private String sentiment;
    private Integer rating;
    private String feedbackText;
    private String selectedChips;
    private String productImageUrl;
    private LocalDateTime createdAt;
    private Boolean isApproved;
    private String emailConfirm;
}
