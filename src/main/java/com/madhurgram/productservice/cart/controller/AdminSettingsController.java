package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.service.AbandonedCartService;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing admin configuration for auto-recovery cart features.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/settings/auto-recovery")
@Tag(name = "Admin — Cart Settings", description = "Settings to manage automated abandoned cart recovery")
public class AdminSettingsController {

    private final AbandonedCartService service;
    private final AuditLogService auditLogService;

    /**
     * Constructor injection for AdminSettingsController.
     *
     * @param service          abandoned cart service
     * @param auditLogService  audit logger service
     */
    public AdminSettingsController(AbandonedCartService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves status of automated autopilot recovery campaigns.
     *
     * @return map detailing enabled status
     */
    @GetMapping
    @Operation(summary = "Get Auto-Recovery status", description = "Retrieves configuration specifying if automated recovery messages/emails are enabled.")
    public ResponseEntity<Map<String, Boolean>> getAutoRecoveryStatus() {
        log.info("Admin request: fetch Auto-Pilot Recovery status");
        boolean enabled = service.isAutoRecoveryEnabled();
        log.info("Auto-Pilot Recovery status is: {}", enabled);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    /**
     * Configures the status of automated autopilot recovery campaigns.
     * Logs action to secure admin logs.
     *
     * @param enabled true to enable, false to disable
     * @return map detailing updated enabled status
     */
    @PostMapping
    @Operation(summary = "Configure Auto-Recovery status", description = "Toggles automated checkout follow-up communications and writes an audit event.")
    public ResponseEntity<Map<String, Boolean>> setAutoRecoveryStatus(@RequestParam boolean enabled) {
        log.info("Admin request: update Auto-Pilot Recovery status to: {}", enabled);
        service.setAutoRecoveryEnabled(enabled);
        
        auditLogService.log("TOGGLE_AUTO_RECOVERY", null, "Auto-Pilot recovery status changed to: " + enabled);
        
        log.info("Auto-Pilot Recovery successfully updated to: {}", enabled);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
}
