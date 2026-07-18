package com.madhurgram.productservice.bundle.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** Request DTO for creating or updating a bundle from the admin panel. */
public record BundleRequestDTO(

        @NotBlank(message = "Tab name is required")
        @Size(max = 100, message = "Tab name must be 100 characters or less")
        String tabName,

        @NotBlank(message = "Bundle name is required")
        @Size(max = 255, message = "Bundle name must be 255 characters or less")
        String name,

        String description,

        @NotNull(message = "Discount percent is required")
        @Min(value = 1, message = "Discount must be at least 1%")
        @Max(value = 70, message = "Discount cannot exceed 70%")
        Integer discountPercent,

        @NotEmpty(message = "At least one product must be included in a bundle")
        @Size(min = 2, message = "A bundle must have at least 2 products")
        List<Long> productIds,

        Integer displayOrder
) {}
