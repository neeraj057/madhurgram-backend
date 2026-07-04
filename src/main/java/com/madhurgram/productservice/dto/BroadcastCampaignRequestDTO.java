package com.madhurgram.productservice.dto;

public record BroadcastCampaignRequestDTO(
    String title,
    String message,
    String targetSegment,
    String productKeyword,
    Long productId
) {}
