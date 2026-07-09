package com.madhurgram.productservice.logistics.provider;

public record LogisticsShipmentResponse(
    String trackingNumber,
    String courierName,
    boolean success
) {}
