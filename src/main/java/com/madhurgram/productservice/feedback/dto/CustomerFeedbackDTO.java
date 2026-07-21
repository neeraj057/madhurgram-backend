package com.madhurgram.productservice.feedback.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFeedbackDTO {
    private Long id;
    private Long orderId;
    
    @NotBlank(message = "Customer name is mandatory")
    private String customerName;
    
    @NotBlank(message = "Sentiment is mandatory")
    private String sentiment;
    
    @NotNull(message = "Rating is mandatory")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;
    
    private String feedbackText;
    private String selectedChips;
    private String productImageUrl;
    private LocalDateTime createdAt;
    private Boolean isApproved;
    private String emailConfirm;
}
