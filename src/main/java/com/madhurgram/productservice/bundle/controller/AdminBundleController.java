package com.madhurgram.productservice.bundle.controller;

import com.madhurgram.productservice.bundle.dto.BundleRequestDTO;
import com.madhurgram.productservice.bundle.dto.BundleResponseDTO;
import com.madhurgram.productservice.bundle.service.BundleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for managing product bundles and footer section mode.
 * All routes are secured by JWT in the security config.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin – Bundle Manager", description = "Create and manage combo bundles and footer section mode")
public class AdminBundleController {

    private final BundleService bundleService;
    private final com.madhurgram.productservice.bundle.service.BundleCopyGeneratorService copyGeneratorService;

    // ──────────── Bundle CRUD ────────────

    @GetMapping("/api/admin/bundles")
    @Operation(summary = "Get all bundles (admin)", description = "Returns all bundles including inactive ones")
    public ResponseEntity<List<BundleResponseDTO>> getAllBundles() {
        return ResponseEntity.ok(bundleService.getAllBundles());
    }

    @PostMapping("/api/admin/bundles")
    @Operation(summary = "Create a new bundle")
    public ResponseEntity<BundleResponseDTO> createBundle(@Valid @RequestBody BundleRequestDTO request) {
        BundleResponseDTO created = bundleService.createBundle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/bundles/{id}")
    @Operation(summary = "Update an existing bundle")
    public ResponseEntity<BundleResponseDTO> updateBundle(
            @PathVariable Long id,
            @Valid @RequestBody BundleRequestDTO request) {
        return ResponseEntity.ok(bundleService.updateBundle(id, request));
    }

    @PatchMapping("/api/admin/bundles/{id}/toggle")
    @Operation(summary = "Toggle bundle active/inactive")
    public ResponseEntity<BundleResponseDTO> toggleBundle(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.toggleActive(id));
    }

    @DeleteMapping("/api/admin/bundles/{id}")
    @Operation(summary = "Delete a bundle")
    public ResponseEntity<Map<String, String>> deleteBundle(@PathVariable Long id) {
        bundleService.deleteBundle(id);
        return ResponseEntity.ok(Map.of("message", "Bundle deleted successfully"));
    }

    // ──────────── Footer Mode ────────────

    @GetMapping("/api/admin/footer-mode")
    @Operation(summary = "Get current footer section mode")
    public ResponseEntity<Map<String, String>> getFooterMode() {
        return ResponseEntity.ok(Map.of("mode", bundleService.getFooterMode()));
    }

    @PutMapping("/api/admin/footer-mode")
    @Operation(summary = "Set footer section mode (COMBOS or BRAND_STORY)")
    public ResponseEntity<Map<String, String>> setFooterMode(@RequestBody Map<String, String> body) {
        String mode = body.getOrDefault("mode", "BRAND_STORY").toUpperCase();
        bundleService.setFooterMode(mode);
        return ResponseEntity.ok(Map.of("mode", mode, "message", "Footer mode updated successfully"));
    }

    // ──────────── Auto-Generator ────────────

    @PostMapping("/api/admin/bundles/generate-copy")
    @Operation(summary = "Auto-generate bundle copy (Rule-Based or AI)")
    public ResponseEntity<com.madhurgram.productservice.bundle.dto.BundleCopyResponseDTO> generateCopy(
            @Valid @RequestBody com.madhurgram.productservice.bundle.dto.BundleCopyRequestDTO request) {
        return ResponseEntity.ok(copyGeneratorService.generateCopy(request));
    }
}
