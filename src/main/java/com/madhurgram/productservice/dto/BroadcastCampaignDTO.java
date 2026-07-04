package com.madhurgram.productservice.dto;

import java.time.LocalDateTime;

public record BroadcastCampaignDTO(
    Long id,
    String title,
    String message,
    String targetSegment,
    String productKeyword,
    Long productId,
    int recipients,
    int conversions,
    LocalDateTime createdAt
) {}
