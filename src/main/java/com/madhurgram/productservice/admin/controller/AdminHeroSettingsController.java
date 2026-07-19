package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.admin.service.AdminHeroSettingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for dynamic hero homepage configurations.
 */
@Slf4j
@RestController
@Tag(name = "Admin — Hero Settings", description = "Endpoints to manage home page hero section configuration")
public class AdminHeroSettingsController {

    private final AdminHeroSettingsService adminHeroSettingsService;

    public AdminHeroSettingsController(AdminHeroSettingsService adminHeroSettingsService) {
        this.adminHeroSettingsService = adminHeroSettingsService;
    }

    @GetMapping("/api/v1/public/settings/hero")
    @Operation(summary = "Get Public Hero Section Configuration", description = "Retrieves active content type and active promotional offer tags.")
    public ResponseEntity<Map<String, String>> getPublicHeroSettings() {
        return ResponseEntity.ok(adminHeroSettingsService.getHeroSettings());
    }

    @GetMapping("/api/v1/admin/settings/hero")
    @Operation(summary = "Get Admin Hero Section Configuration", description = "Admin endpoint to fetch configurations.")
    public ResponseEntity<Map<String, String>> getAdminHeroSettings() {
        return ResponseEntity.ok(adminHeroSettingsService.getHeroSettings());
    }

    @PutMapping("/api/v1/admin/settings/hero")
    @Operation(summary = "Update Hero Section Configuration", description = "Updates configurations and logs audit entry.")
    public ResponseEntity<Map<String, String>> updateHeroSettings(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(adminHeroSettingsService.updateHeroSettings(payload));
    }
}
