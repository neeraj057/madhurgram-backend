package com.madhurgram.productservice.marketing.dto;

import java.util.Locale;

public enum MarketingSegment {
    TOP_SPENDERS("top spenders"),
    INACTIVE_CUSTOMERS("inactive customers"),
    OIL_BUYERS("oil buyers");

    private final String value;

    MarketingSegment(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MarketingSegment fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Target segment is required");
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "top spenders" -> TOP_SPENDERS;
            case "inactive customers" -> INACTIVE_CUSTOMERS;
            case "oil buyers" -> OIL_BUYERS;
            default -> throw new IllegalArgumentException("Unsupported target segment: " + value);
        };
    }
}
