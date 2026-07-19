package com.madhurgram.productservice.logistics.provider.impl;

import com.madhurgram.productservice.common.constants.SystemSettingKeys;
import com.madhurgram.productservice.common.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages Shiprocket JWT authentication token lifecycle.
 * Token is fetched once, cached in memory for 9 days, and auto-renewed on expiry or 401.
 * Credentials are read from system_settings table configurable via Admin Panel without redeployment.
 */
@Slf4j
@Service
public class ShiprocketAuthServiceImpl implements com.madhurgram.productservice.logistics.provider.ShiprocketAuthService {

    private static final String SHIPROCKET_AUTH_URL = "https://apiv2.shiprocket.in/v1/external/auth/login";
    private static final long TOKEN_VALIDITY_SECONDS = 9L * 24 * 60 * 60;

    private final SystemSettingService settingService;
    private final RestTemplate restTemplate;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public ShiprocketAuthServiceImpl(SystemSettingService settingService) {
        this.settingService = settingService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Returns a valid Shiprocket JWT token. Fetches fresh if expired or missing.
     */
    public String getToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        return refreshToken();
    }

    /**
     * Forces a fresh token fetch. Called on 401 responses from Shiprocket APIs.
     */
    public String refreshToken() {
        tokenLock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            String email = settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_EMAIL, "");
            String password = settingService.getSettingValue(SystemSettingKeys.SHIPROCKET_PASSWORD, "");
            if (email.isBlank() || password.isBlank()) {
                log.warn("[ShiprocketAuth] Credentials not configured in system_settings. Skipping auth.");
                return null;
            }
            log.info("[ShiprocketAuth] Refreshing Shiprocket JWT for: {}", email);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("email", email, "password", password);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(SHIPROCKET_AUTH_URL, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String token = (String) response.getBody().get("token");
                if (token != null && !token.isBlank()) {
                    this.cachedToken = token;
                    this.tokenExpiresAt = Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS);
                    log.info("[ShiprocketAuth] Token refreshed. Expires at: {}", tokenExpiresAt);
                    return token;
                }
            }
            log.error("[ShiprocketAuth] Failed to fetch token. Status: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("[ShiprocketAuth] Exception during token refresh: {}", e.getMessage());
            return null;
        } finally {
            tokenLock.unlock();
        }
    }
}