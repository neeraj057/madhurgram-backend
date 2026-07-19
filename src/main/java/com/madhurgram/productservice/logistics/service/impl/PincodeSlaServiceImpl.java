package com.madhurgram.productservice.logistics.service.impl;

import com.madhurgram.productservice.audit.service.AuditLogService;
import com.madhurgram.productservice.common.constants.SystemSettingKeys;
import com.madhurgram.productservice.common.service.SystemSettingService;
import com.madhurgram.productservice.logistics.dto.AdminPincodeSettingsDto;
import com.madhurgram.productservice.logistics.dto.PincodeSlaResponseDto;
import com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto;
import com.madhurgram.productservice.logistics.dto.ShiprocketSettingsDto;
import com.madhurgram.productservice.logistics.provider.ShiprocketServiceabilityService;
import com.madhurgram.productservice.logistics.service.PincodeSlaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PincodeSlaServiceImpl implements PincodeSlaService {

    private final SystemSettingService settingService;
    private final AuditLogService auditLogService;
    private final ShiprocketServiceabilityService shiprocketServiceabilityService;
    private final RestTemplate restTemplate;

    public PincodeSlaServiceImpl(SystemSettingService settingService,
                                 AuditLogService auditLogService,
                                 ShiprocketServiceabilityService shiprocketServiceabilityService) {
        this.settingService = settingService;
        this.auditLogService = auditLogService;
        this.shiprocketServiceabilityService = shiprocketServiceabilityService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1500);
        factory.setReadTimeout(1500);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public PincodeSlaResponseDto checkPincode(String pincode) {
        PincodeSlaResponseDto.PincodeSlaResponseDtoBuilder responseBuilder = PincodeSlaResponseDto.builder();

        if (pincode == null || !pincode.matches("^[1-9][0-9]{5}$")) {
            return responseBuilder
                    .available(false)
                    .message("Please enter a valid 6-digit Indian pincode.")
                    .build();
        }

        String localSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_LOCAL, "1-2 Business Days");
        String regionalSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_REGIONAL, "2-3 Business Days");
        String nationalSla = settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_NATIONAL, "4-6 Business Days");

        boolean shiprocketEnabled = settingService.getSettingBoolean(SystemSettingKeys.SHIPROCKET_SERVICEABILITY_ENABLED, false);
        if (shiprocketEnabled) {
            try {
                ServiceabilityResultDto result = shiprocketServiceabilityService.checkServiceability(pincode);
                if (result != null) {
                    if (!result.available()) {
                        return responseBuilder
                                .available(false)
                                .message("Sorry, delivery is currently not available for this pincode.")
                                .disclaimer("Please contact us for alternative delivery arrangements.")
                                .build();
                    }
                    String sla = result.estimatedDays() != null ? result.estimatedDays() : nationalSla;
                    log.info("[PincodeCheck] Shiprocket: pincode={}, courier={}, COD={}", pincode, result.courierName(), result.codAvailable());
                    return responseBuilder
                            .available(true)
                            .sla(sla)
                            .cod(result.codAvailable())
                            .courier(result.courierName() != null ? result.courierName() : "Courier Partner")
                            .message("Estimated delivery: " + sla)
                            .disclaimer(result.codAvailable() ? "Real-time courier check. COD available." : "Real-time courier check. Prepaid payment only.")
                            .build();
                }
                log.warn("[PincodeCheck] Shiprocket null result for {}. Falling back.", pincode);
            } catch (Exception e) {
                log.error("[PincodeCheck] Shiprocket error for {}: {}. Falling back.", pincode, e.getMessage());
            }
        }

        try {
            String url = "https://api.postalpincode.in/pincode/" + pincode;
            List<?> rawList = restTemplate.getForObject(url, List.class);
            if (rawList != null && !rawList.isEmpty()) {
                Map<?, ?> responseMap = (Map<?, ?>) rawList.getFirst();
                if ("Success".equalsIgnoreCase((String) responseMap.get("Status"))) {
                    List<?> postOfficeList = (List<?>) responseMap.get("PostOffice");
                    if (postOfficeList != null && !postOfficeList.isEmpty()) {
                        Map<?, ?> postOffice = (Map<?, ?>) postOfficeList.getFirst();
                        String district = ((String) postOffice.get("District")).trim();
                        String state = ((String) postOffice.get("State")).trim();
                        responseBuilder.location(district + ", " + state);
                        return buildZoneResponse(responseBuilder, district, state, localSla, regionalSla, nationalSla);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PincodeCheck] Postal API timed out for {}. Using digit-pattern fallback.", pincode);
        }

        return buildZoneResponseByPattern(responseBuilder, pincode, localSla, regionalSla, nationalSla);
    }

    private PincodeSlaResponseDto buildZoneResponse(PincodeSlaResponseDto.PincodeSlaResponseDtoBuilder builder,
                                                    String district, String state,
                                                    String localSla, String regionalSla, String nationalSla) {
        builder.available(true);
        if ("Bhadohi".equalsIgnoreCase(district) || "Varanasi".equalsIgnoreCase(district)) {
            return builder.tier("LOCAL").sla(localSla).cod(true)
                    .courier("Shiprocket Express / Self Delivery")
                    .message("Estimated delivery: " + localSla)
                    .disclaimer("Timeline is an estimate based on your zone.").build();
        } else if ("Uttar Pradesh".equalsIgnoreCase(state)) {
            return builder.tier("REGIONAL").sla(regionalSla).cod(true)
                    .courier("Shiprocket Express")
                    .message("Estimated delivery: " + regionalSla)
                    .disclaimer("Timeline is an estimate. COD available.").build();
        } else {
            return builder.tier("NATIONAL").sla(nationalSla).cod(false)
                    .courier("Delhivery / BlueDart")
                    .message("Estimated delivery: " + nationalSla)
                    .disclaimer("COD not available for this location. Prepaid only.").build();
        }
    }

    private PincodeSlaResponseDto buildZoneResponseByPattern(PincodeSlaResponseDto.PincodeSlaResponseDtoBuilder builder,
                                                             String pincode,
                                                             String localSla, String regionalSla, String nationalSla) {
        char firstChar = pincode.charAt(0);
        String threeChars = pincode.length() >= 3 ? pincode.substring(0, 3) : "";
        builder.available(true);
        if ("221".equals(threeChars)) {
            return builder.tier("LOCAL").sla(localSla).cod(true)
                    .courier("Shiprocket Express / Self Delivery")
                    .message("Estimated delivery: " + localSla)
                    .disclaimer("Timeline is an estimate based on your zone.").build();
        } else if (firstChar == '2') {
            return builder.tier("REGIONAL").sla(regionalSla).cod(true)
                    .courier("Shiprocket Express")
                    .message("Estimated delivery: " + regionalSla)
                    .disclaimer("Timeline is an estimate. COD available.").build();
        } else {
            return builder.tier("NATIONAL").sla(nationalSla).cod(false)
                    .courier("Delhivery / BlueDart")
                    .message("Estimated delivery: " + nationalSla)
                    .disclaimer("COD not available for this location. Prepaid only.").build();
        }
    }

    @Override
    public AdminPincodeSettingsDto getAdminPincodeSettings() {
        AdminPincodeSettingsDto dto = new AdminPincodeSettingsDto();
        dto.setPincodeSlaLocal(settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_LOCAL, "1-2 Business Days"));
        dto.setPincodeSlaRegional(settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_REGIONAL, "2-3 Business Days"));
        dto.setPincodeSlaNational(settingService.getSettingValue(SystemSettingKeys.PINCODE_SLA_NATIONAL, "4-6 Business Days"));
        return dto;
    }

    @Override
    public AdminPincodeSettingsDto updateAdminPincodeSettings(AdminPincodeSettingsDto payload) {
        String local = payload.getPincodeSlaLocal() != null ? payload.getPincodeSlaLocal() : "1-2 Business Days";
        String regional = payload.getPincodeSlaRegional() != null ? payload.getPincodeSlaRegional() : "2-3 Business Days";
        String national = payload.getPincodeSlaNational() != null ? payload.getPincodeSlaNational() : "4-6 Business Days";
        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_LOCAL, local, "SLA for Bhadohi/Varanasi districts");
        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_REGIONAL, regional, "SLA for Uttar Pradesh region");
        settingService.saveSetting(SystemSettingKeys.PINCODE_SLA_NATIONAL, national, "SLA for rest of India");
        auditLogService.log("UPDATE_PINCODE_SLA_SETTINGS", null, "Pincode SLA configs updated by Admin.");
        return getAdminPincodeSettings();
    }

    @Override
    public ShiprocketSettingsDto getShiprocketSettings() {
        ShiprocketSettingsDto dto = new ShiprocketSettingsDto();
        dto.setShiprocketEnabled(settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_SERVICEABILITY_ENABLED, "false"));
        dto.setShiprocketEmail(settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_EMAIL, ""));
        String pwd = settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_PASSWORD, "");
        dto.setShiprocketPasswordConfigured(String.valueOf(!pwd.isBlank()));
        dto.setShiprocketPickupPincode(settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_PICKUP_PINCODE, "221303"));
        return dto;
    }

    @Override
    public ShiprocketSettingsDto updateShiprocketSettings(ShiprocketSettingsDto payload) {
        if (payload.getShiprocketEnabled() != null)
            settingService.saveSetting(SystemSettingKeys.SHIPROCKET_SERVICEABILITY_ENABLED, payload.getShiprocketEnabled(), "Enable/disable Shiprocket serviceability check");
        if (payload.getShiprocketEmail() != null)
            settingService.saveSetting(SystemSettingKeys.SHIPROCKET_EMAIL, payload.getShiprocketEmail(), "Shiprocket account email");
        if (payload.getShiprocketPassword() != null)
            settingService.saveSetting(SystemSettingKeys.SHIPROCKET_PASSWORD, payload.getShiprocketPassword(), "Shiprocket account password");
        if (payload.getShiprocketPickupPincode() != null)
            settingService.saveSetting(SystemSettingKeys.SHIPROCKET_PICKUP_PINCODE, payload.getShiprocketPickupPincode(), "Warehouse pickup pincode");
        auditLogService.log("UPDATE_SHIPROCKET_SETTINGS", null, "Shiprocket integration settings updated by Admin.");
        return getShiprocketSettings();
    }
}