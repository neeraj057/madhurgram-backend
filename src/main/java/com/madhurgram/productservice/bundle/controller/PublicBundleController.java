package com.madhurgram.productservice.bundle.controller;

import com.madhurgram.productservice.bundle.dto.PublicFooterSectionDTO;
import com.madhurgram.productservice.bundle.service.BundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public storefront endpoints for bundles and footer section.
 * Accessible without authentication.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Public – Bundles & Footer", description = "Storefront API for footer mode and active bundles")
public class PublicBundleController {

    private final BundleService bundleService;

    @GetMapping("/api/public/footer")
    @Operation(summary = "Get Footer Section Config", description = "Returns footer mode (COMBOS/BRAND_STORY) and active bundles if applicable")
    public ResponseEntity<PublicFooterSectionDTO> getFooterSection() {
        return ResponseEntity.ok(bundleService.getPublicFooterSection());
    }
}
