package com.madhurgram.productservice.common.controller;

import com.madhurgram.productservice.common.constants.SystemSettingKeys;
import com.madhurgram.productservice.common.service.SystemSettingService;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing WhatsApp Quick Buy configurations using standard service layers.
 */
@Slf4j
@RestController
@Tag(name = "Admin — WhatsApp Settings", description = "Endpoints to manage WhatsApp Quick Buy toggle and templates")
public class WhatsAppSettingsController {

    private final SystemSettingService settingService;
    private final AuditLogService auditLogService;

    public WhatsAppSettingsController(SystemSettingService settingService, AuditLogService auditLogService) {
        this.settingService = settingService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/public/settings/whatsapp")
    @Operation(summary = "Get Public WhatsApp Config", description = "Retrieves configurations for WhatsApp Quick Buy button.")
    public ResponseEntity<Map<String, String>> getPublicWhatsAppSettings() {
        Map<String, String> settings = new HashMap<>();
        
        settings.put("whatsappEnabled", settingService.getSettingValue(SystemSettingKeys.WHATSAPP_QUICK_BUY_ENABLED, "true"));
        settings.put("whatsappNumber", settingService.getSettingValue(SystemSettingKeys.WHATSAPP_QUICK_BUY_NUMBER, "917899999902"));
        settings.put("whatsappTemplate", settingService.getSettingValue(SystemSettingKeys.WHATSAPP_QUICK_BUY_TEXT_TEMPLATE, 
                "Hello MadhurGram,\n\nMy name is {custName}.\nI want to order *{productName}* ({volume}) - {quantity} unit(s).\nMy delivery address is: {custAddress}.\n\nPlease confirm my order."));
        
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

        settingService.saveSetting(SystemSettingKeys.WHATSAPP_QUICK_BUY_ENABLED, enabled, "Toggle WhatsApp Quick Buy button on checkout/product grids");
        settingService.saveSetting(SystemSettingKeys.WHATSAPP_QUICK_BUY_NUMBER, number, "WhatsApp business contact phone number with country code");
        settingService.saveSetting(SystemSettingKeys.WHATSAPP_QUICK_BUY_TEXT_TEMPLATE, template, "Default pre-filled message text template for quick order");

        auditLogService.log("UPDATE_WHATSAPP_SETTINGS", null, "WhatsApp config updated. Enabled: " + enabled + ", Contact: " + number);
        log.info("WhatsApp configurations updated by admin. Enabled: {}, Contact: {}", enabled, number);

        return getPublicWhatsAppSettings();
    }
}
