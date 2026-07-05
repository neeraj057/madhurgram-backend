package com.madhurgram.productservice.cart.controller;

import com.madhurgram.productservice.cart.service.AbandonedCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings/auto-recovery")
@CrossOrigin(origins = "*")
public class AdminSettingsController {

    private static final Logger log = LoggerFactory.getLogger(AdminSettingsController.class);
    private final AbandonedCartService service;

    public AdminSettingsController(AbandonedCartService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getAutoRecoveryStatus() {
        log.info("Received request to fetch Auto-Pilot Recovery status");
        boolean enabled = service.isAutoRecoveryEnabled();
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> setAutoRecoveryStatus(@RequestParam boolean enabled) {
        log.info("Received request to update Auto-Pilot Recovery status to: {}", enabled);
        service.setAutoRecoveryEnabled(enabled);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
}
