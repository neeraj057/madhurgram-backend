package com.madhurgram.productservice.common.controller;

import com.madhurgram.productservice.common.constants.SystemSettingKeys;
import com.madhurgram.productservice.common.service.SystemSettingService;
import com.madhurgram.productservice.audit.service.AuditLogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller to verify Indian PIN codes, calculate delivery SLA tiers (Local, Regional, National),
 * and manage delivery SLAs dynamically using service layers.
 */
@Slf4j
@RestController
@Tag(name = "Admin — Pincode Delivery SLA", description = "Endpoints to manage and calculate courier delivery timelines")
public class PincodeSlaController {

    private final SystemSettingService settingService;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate;

    public PincodeSlaController(SystemSettingService settingService, AuditLogService auditLogService) {
        this.settingService = settingService;
        this.auditLogService = auditLogService;
        
        // Setup RestTemplate with 1.5 second timeout to prevent thread hanging on external API call
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(1500);
        this.restTemplate = new RestTemplate(factory);
    }

    @GetMapping("/api/public/pincode/check")
    @Operation(summary = "Check Pincode Delivery Availability & SLA", description = "Checks Pincode legitimacy and maps expected SLA dynamically.")
    public ResponseEntity<Map<String, Object>> checkPincode(@RequestParam String pincode) {
        Map<String, Object> response = new HashMap<>();
        
        // 1. Sanity check: must be a 6-digit Indian PIN code
        if (pincode == null || !pincode.matches("^[1-9][0-9]{5}$")) {
            response.put("available", false);
            response.put("message", "Invalid Indian pincode format.");
            return ResponseEntity.ok(response);
        }

        String localSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_LOCAL, "1-2 Business Days");
        String regionalSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_REGIONAL, "2-3 Business Days");
        String nationalSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_NATIONAL, "4-6 Business Days");

        // 2. Query the postal directory API to resolve District and State
        try {
            String url = "https://api.postalpincode.in/pincode/" + pincode;
            List<?> rawList = restTemplate.getForObject(url, List.class);
            
            if (rawList != null && !rawList.isEmpty()) {
                Map<?, ?> responseMap = (Map<?, ?>) rawList.getFirst();
                String status = (String) responseMap.get("Status");
                
                if ("Success".equalsIgnoreCase(status)) {
                    List<?> postOfficeList = (List<?>) responseMap.get("PostOffice");
                    if (postOfficeList != null && !postOfficeList.isEmpty()) {
                        Map<?, ?> postOffice = (Map<?, ?>) postOfficeList.getFirst();
                        String district = ((String) postOffice.get("District")).trim();
                        String state = ((String) postOffice.get("State")).trim();
                        
                        // Map location details to SLA tiers
                        if ("Bhadohi".equalsIgnoreCase(district) || "Varanasi".equalsIgnoreCase(district)) {
                            response.put("available", true);
                            response.put("tier", "LOCAL");
                            response.put("sla", localSla);
                            response.put("message", "Express delivery available from Bhadohi warehouse.");
                        } else if ("Uttar Pradesh".equalsIgnoreCase(state)) {
                            response.put("available", true);
                            response.put("tier", "REGIONAL");
                            response.put("sla", regionalSla);
                            response.put("message", "Courier delivery available via Shiprocket Express.");
                        } else {
                            response.put("available", true);
                            response.put("tier", "NATIONAL");
                            response.put("sla", nationalSla);
                            response.put("message", "Courier delivery available via Delhivery/BlueDart.");
                        }
                        
                        response.put("location", district + ", " + state);
                        return ResponseEntity.ok(response);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("External Postal Pincode API lookup timed out or failed. Falling back to default pattern mapping.", e);
        }

        // 3. Fallback logic: If external API fails, resolve tier locally based on starting digit of pincode
        char firstChar = pincode.charAt(0);
        String secondChar = pincode.substring(0, 2);
        
        response.put("available", true);
        if ("22".equals(secondChar)) {
            response.put("tier", "LOCAL");
            response.put("sla", localSla);
            response.put("message", "Express delivery available from Bhadohi warehouse.");
        } else if (firstChar == '2') {
            response.put("tier", "REGIONAL");
            response.put("sla", regionalSla);
            response.put("message", "Courier delivery available via Shiprocket Express.");
        } else {
            response.put("tier", "NATIONAL");
            response.put("sla", nationalSla);
            response.put("message", "Courier delivery available via Delhivery/BlueDart.");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/admin/settings/pincode")
    @Operation(summary = "Get Pincode SLA Configs", description = "Admin endpoint to fetch delivery SLA durations.")
    public ResponseEntity<Map<String, String>> getAdminPincodeSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("pincodeSlaLocal", settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_LOCAL, "1-2 Business Days"));
        settings.put("pincodeSlaRegional", settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_REGIONAL, "2-3 Business Days"));
        settings.put("pincodeSlaNational", settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_NATIONAL, "4-6 Business Days"));
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/api/admin/settings/pincode")
    @Operation(summary = "Update Pincode SLA Configs", description = "Updates expected delivery timelines for each tier.")
    public ResponseEntity<Map<String, String>> updateAdminPincodeSettings(@RequestBody Map<String, String> payload) {
        String local = payload.getOrDefault("pincodeSlaLocal", "1-2 Business Days");
        String regional = payload.getOrDefault("pincodeSlaRegional", "2-3 Business Days");
        String national = payload.getOrDefault("pincodeSlaNational", "4-6 Business Days");

        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_LOCAL, local, "Expected courier delivery SLA for Bhadohi and Varanasi districts");
        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_REGIONAL, regional, "Expected courier delivery SLA for Uttar Pradesh region");
        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_NATIONAL, national, "Expected courier delivery SLA for rest of India");

        auditLogService.log("UPDATE_PINCODE_SLA_SETTINGS", null, "Pincode SLA configs updated by Admin.");
        log.info("Pincode SLA configurations updated by Admin: Local: {}, Regional: {}, National: {}", local, regional, national);

        return getAdminPincodeSettings();
    }
}
