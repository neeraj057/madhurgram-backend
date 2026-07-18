package com.madhurgram.productservice.bundle.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BundleCopyRequestDTO(
        @NotEmpty(message = "Product IDs are required")
        List<Long> productIds,

        @NotNull(message = "Engine type is required")
        EngineType engine
) {
    public enum EngineType {
        RULE_BASED,
        AI
    }
}
