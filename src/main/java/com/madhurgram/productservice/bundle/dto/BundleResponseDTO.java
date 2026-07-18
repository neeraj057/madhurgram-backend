package com.madhurgram.productservice.bundle.dto;

import java.math.BigDecimal;
import java.util.List;

/** Response DTO sent to both admin and storefront. */
public record BundleResponseDTO(
        Long id,
        String tabName,
        String name,
        String description,
        Integer discountPercent,
        boolean active,
        Integer displayOrder,
        BigDecimal originalPrice,   // sum of all item prices
        BigDecimal bundlePrice,     // originalPrice minus discount
        BigDecimal savings,         // how much customer saves
        List<BundleItemDTO> items
) {
    public record BundleItemDTO(
            Long productId,
            String name,
            BigDecimal price,
            String volume,
            String imageUrl,
            Integer stock
    ) {}
}
