package com.madhurgram.productservice.logistics.controller;

import com.madhurgram.productservice.logistics.dto.AdminPincodeSettingsDto;
import com.madhurgram.productservice.logistics.dto.PincodeSlaResponseDto;
import com.madhurgram.productservice.logistics.dto.ShiprocketSettingsDto;
import com.madhurgram.productservice.logistics.service.PincodeSlaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Admin — Pincode Delivery SLA", description = "Endpoints to manage and calculate courier delivery timelines")
public class PincodeSlaController {

    private final PincodeSlaService pincodeSlaService;

    public PincodeSlaController(PincodeSlaService pincodeSlaService) {
        this.pincodeSlaService = pincodeSlaService;
    }

    @GetMapping("/api/public/pincode/check")
    @Operation(summary = "Check Pincode Delivery Availability & SLA")
    public ResponseEntity<PincodeSlaResponseDto> checkPincode(@RequestParam String pincode) {
        return ResponseEntity.ok(pincodeSlaService.checkPincode(pincode));
    }

    @GetMapping("/api/admin/settings/pincode")
    @Operation(summary = "Get Pincode SLA Configs")
    public ResponseEntity<AdminPincodeSettingsDto> getAdminPincodeSettings() {
        return ResponseEntity.ok(pincodeSlaService.getAdminPincodeSettings());
    }

    @PutMapping("/api/admin/settings/pincode")
    @Operation(summary = "Update Pincode SLA Configs")
    public ResponseEntity<AdminPincodeSettingsDto> updateAdminPincodeSettings(@RequestBody AdminPincodeSettingsDto payload) {
        return ResponseEntity.ok(pincodeSlaService.updateAdminPincodeSettings(payload));
    }

    @GetMapping("/api/admin/settings/shiprocket")
    @Operation(summary = "Get Shiprocket Config")
    public ResponseEntity<ShiprocketSettingsDto> getShiprocketSettings() {
        return ResponseEntity.ok(pincodeSlaService.getShiprocketSettings());
    }

    @PutMapping("/api/admin/settings/shiprocket")
    @Operation(summary = "Update Shiprocket Config")
    public ResponseEntity<ShiprocketSettingsDto> updateShiprocketSettings(@RequestBody ShiprocketSettingsDto payload) {
        return ResponseEntity.ok(pincodeSlaService.updateShiprocketSettings(payload));
    }
}