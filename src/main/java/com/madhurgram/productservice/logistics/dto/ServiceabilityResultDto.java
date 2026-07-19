package com.madhurgram.productservice.logistics.dto;

public record ServiceabilityResultDto(
        boolean available,
        String courierName,
        String estimatedDays,
        boolean codAvailable,
        String rawEtd
) {}