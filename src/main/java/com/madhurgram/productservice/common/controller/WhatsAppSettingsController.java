package com.madhurgram.productservice.common.controller;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing WhatsApp Quick Buy configurations.
 */
@Slf4j
@RestController
@Tag(name = "Admin — WhatsApp Settings", description = "Endpoints to manage WhatsApp Quick Buy toggle and templates")
public class WhatsAppSettingsController {

    private final SystemSettingRepository repository;
    private final AuditLogService auditLogService;

    public WhatsAppSettingsController(SystemSettingRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    private String getSettingOrDefault(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    private void saveSetting(String key, String value, String description) {
        SystemSetting setting = repository.findById(key)
                .orElse(SystemSetting.builder()
                        .settingKey(key)
                        .description(description)
                        .build());
        setting.setSettingValue(value);
        repository.save(setting);
    }

    @GetMapping("/api/public/settings/whatsapp")
    @Operation(summary = "Get Public WhatsApp Config", description = "Retrieves configurations for WhatsApp Quick Buy button.")
    public ResponseEntity<Map<String, String>> getPublicWhatsAppSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("whatsappEnabled", getSettingOrDefault("WHATSAPP_QUICK_BUY_ENABLED", "true"));
        settings.put("whatsappNumber", getSettingOrDefault("WHATSAPP_QUICK_BUY_NUMBER", "917899999902"));
        settings.put("whatsappTemplate", getSettingOrDefault("WHATSAPP_QUICK_BUY_TEXT_TEMPLATE", 
                "Hello MadhurGram,\n\nMy name is {custName}.\nI want to order *{productName}* ({volume}).\nMy delivery address is: {custAddress}.\n\nPlease confirm my order."));
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/api/admin/settings/whatsapp")
    @Operation(summary = "Get Admin WhatsApp Config", description = "Admin endpoint to fetch configurations.")
    public ResponseEntity<Map<String, String>> getAdminWhatsAppSettings() {
        return getPublicWhatsAppSettings();
    }

    @PutMapping("/api/admin/settings/whatsapp")
    @Operation(summary = "Update WhatsApp Config", description = "Updates configurations for WhatsApp Quick Buy and logs audit entry.")
    public ResponseEntity<Map<String, String>> updateWhatsAppSettings(@RequestBody Map<String, String> payload) {
        String enabled = payload.getOrDefault("whatsappEnabled", "true");
        String number = payload.getOrDefault("whatsappNumber", "917899999902");
        String template = payload.getOrDefault("whatsappTemplate", "");

        saveSetting("WHATSAPP_QUICK_BUY_ENABLED", enabled, "Toggle WhatsApp Quick Buy button on checkout/product grids");
        saveSetting("WHATSAPP_QUICK_BUY_NUMBER", number, "WhatsApp business contact phone number with country code");
        saveSetting("WHATSAPP_QUICK_BUY_TEXT_TEMPLATE", template, "Default pre-filled message text template for quick order");

        auditLogService.log("UPDATE_WHATSAPP_SETTINGS", null, "WhatsApp config updated. Enabled: " + enabled + ", Contact: " + number);
        log.info("WhatsApp configurations updated by admin. Enabled: {}, Contact: {}", enabled, number);

        return getPublicWhatsAppSettings();
    }
}
