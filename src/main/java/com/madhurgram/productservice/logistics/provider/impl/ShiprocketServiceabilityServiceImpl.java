package com.madhurgram.productservice.logistics.provider.impl;

import com.madhurgram.productservice.common.constants.SystemSettingKeys;
import com.madhurgram.productservice.common.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Checks real-time courier serviceability for a given pincode using Shiprocket API.
 * Results are cached in Redis for 6 hours to reduce API calls (Phase 3).
 * Falls back gracefully if Shiprocket is disabled or credentials are not configured.
 */
@Slf4j
@Service
public class ShiprocketServiceabilityServiceImpl implements com.madhurgram.productservice.logistics.provider.ShiprocketServiceabilityService {

    private static final String SERVICEABILITY_URL = "https://apiv2.shiprocket.in/v1/external/courier/serviceability/";
    private static final double DEFAULT_WEIGHT_KG = 0.5;

    private final com.madhurgram.productservice.logistics.provider.ShiprocketAuthService authService;
    private final SystemSettingService settingService;
    private final RestTemplate restTemplate;

    public ShiprocketServiceabilityServiceImpl(com.madhurgram.productservice.logistics.provider.ShiprocketAuthService authService, SystemSettingService settingService) {
        this.authService = authService;
        this.settingService = settingService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(6000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Checks whether Shiprocket can deliver to a given pincode.
     * Result is cached in Redis for 6 hours (cache key = pincode).
     */
    @Cacheable(value = "pincodeServiceability", key = "#deliveryPincode", unless = "#result == null")
    public com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto checkServiceability(String deliveryPincode) {
        log.info("[Shiprocket] Checking serviceability for pincode: {}", deliveryPincode);

        String token = authService.getToken();
        if (token == null) {
            log.warn("[Shiprocket] No auth token available. Skipping serviceability check.");
            return null;
        }

        String pickupPincode = settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_PICKUP_PINCODE, "221303");

        String url = String.format("%s?pickup_postcode=%s&delivery_postcode=%s&weight=%.1f&cod=1",
                SERVICEABILITY_URL, pickupPincode, deliveryPincode, DEFAULT_WEIGHT_KG);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[Shiprocket] Non-2xx response: {}", response.getStatusCode());
                return new com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto(false, null, null, false, null);
            }

            Map<?, ?> body = response.getBody();
            Map<?, ?> data = (Map<?, ?>) body.get("data");
            if (data == null) {
                log.warn("[Shiprocket] No 'data' in serviceability response.");
                return new com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto(false, null, null, false, null);
            }

            List<?> couriers = (List<?>) data.get("available_courier_companies");
            if (couriers == null || couriers.isEmpty()) {
                log.info("[Shiprocket] No couriers available for pincode: {}", deliveryPincode);
                return new com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto(false, null, null, false, null);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> bestCourier = (Map<String, Object>) couriers.stream()
                    .map(c -> (Map<String, Object>) c)
                    .max((a, b) -> Double.compare(toDouble(a.get("rating")), toDouble(b.get("rating"))))
                    .orElse((Map<String, Object>) couriers.get(0));

            String courierName = (String) bestCourier.get("courier_name");
            String etd = (String) bestCourier.get("etd");
            Object codObj = bestCourier.get("cod");
            boolean codAvailable = codObj != null && (codObj.equals(1) || codObj.equals(true) || "1".equals(codObj.toString()));
            String estimatedDays = formatEtd(etd);

            log.info("[Shiprocket] Pincode {}: courier={}, COD={}, ETD={}", deliveryPincode, courierName, codAvailable, etd);
            return new com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto(true, courierName, estimatedDays, codAvailable, etd);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("[Shiprocket] 401 Unauthorized. Refreshing token.");
            authService.refreshToken();
            return null;
        } catch (Exception e) {
            log.error("[Shiprocket] Serviceability API error for pincode {}: {}", deliveryPincode, e.getMessage());
            return null;
        }
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }

    private String formatEtd(String etd) {
        if (etd == null || etd.isBlank()) return null;
        try {
            LocalDate deliveryDate = LocalDate.parse(etd.length() > 10 ? etd.substring(0, 10) : etd);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), deliveryDate);
            if (days <= 0) return "1-2 Business Days";
            return days + (days == 1 ? " Business Day" : " Business Days");
        } catch (Exception e) {
            return etd;
        }
    }


}