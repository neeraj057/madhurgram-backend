package com.madhurgram.productservice.marketing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BroadcastCampaignRequestDTO(
    @NotBlank(message = "Campaign title is required")
    String title,
    @NotBlank(message = "Campaign message is required")
    String message,
    @NotBlank(message = "Target segment is required")
    String targetSegment,
    @NotBlank(message = "Product keyword is required")
    String productKeyword,
    @Positive(message = "Product ID must be a positive number")
    Long productId
) {}
