package com.madhurgram.productservice.bundle.dto;

import java.util.List;

/**
 * Public API response that combines footer section mode + active bundles.
 * One API call gives the storefront everything it needs to render the footer section.
 */
public record PublicFooterSectionDTO(
        /** "COMBOS" or "BRAND_STORY" — admin sets this */
        String footerMode,
        List<BundleResponseDTO> bundles
) {}
