package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import com.madhurgram.productservice.coupon.repository.CouponRepository;
import com.madhurgram.productservice.coupon.entity.Coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controller for dynamic hero homepage configurations (video loop, purity image, or promotional card template).
 */
@Slf4j
@RestController
@Tag(name = "Admin — Hero Settings", description = "Endpoints to manage home page hero section configuration")
public class AdminHeroSettingsController {

    private final SystemSettingRepository repository;
    private final AuditLogService auditLogService;
    private final CouponRepository couponRepository;

    public AdminHeroSettingsController(
            SystemSettingRepository repository, 
            AuditLogService auditLogService, 
            CouponRepository couponRepository) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.couponRepository = couponRepository;
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

    @GetMapping("/api/public/settings/hero")
    @Operation(summary = "Get Public Hero Section Configuration", description = "Retrieves active content type and active promotional offer tags.")
    public ResponseEntity<Map<String, String>> getPublicHeroSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("heroContentType", getSettingOrDefault("HERO_CONTENT_TYPE", "video"));
        settings.put("offerTitle", getSettingOrDefault("HERO_OFFER_TITLE", "Special Inaugural Swadeshi Offer"));
        settings.put("offerSubtitle", getSettingOrDefault("HERO_OFFER_SUBTITLE", "Use coupon code GOPIGANJ10 for 10% off on all organic cow ghee!"));
        settings.put("offerLink", getSettingOrDefault("HERO_OFFER_LINK", "/#products"));
        settings.put("offerCoupon", getSettingOrDefault("HERO_OFFER_COUPON", "GOPIGANJ10"));
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/api/admin/settings/hero")
    @Operation(summary = "Get Admin Hero Section Configuration", description = "Admin endpoint to fetch configurations.")
    public ResponseEntity<Map<String, String>> getAdminHeroSettings() {
        return getPublicHeroSettings();
    }

    @PutMapping("/api/admin/settings/hero")
    @Operation(summary = "Update Hero Section Configuration", description = "Updates configurations for background media and promo tags and logs audit entry.")
    public ResponseEntity<Map<String, String>> updateHeroSettings(@RequestBody Map<String, String> payload) {
        String type = payload.getOrDefault("heroContentType", "video");
        String title = payload.getOrDefault("offerTitle", "");
        String subtitle = payload.getOrDefault("offerSubtitle", "");
        String link = payload.getOrDefault("offerLink", "/#products");
        String coupon = payload.getOrDefault("offerCoupon", "");

        saveSetting("HERO_CONTENT_TYPE", type, "Hero Section media source type (video, image, offer)");
        saveSetting("HERO_OFFER_TITLE", title, "Hero section promo offer headline text");
        saveSetting("HERO_OFFER_SUBTITLE", subtitle, "Hero section promo details text");
        saveSetting("HERO_OFFER_LINK", link, "Hero section action redirection route link");
        saveSetting("HERO_OFFER_COUPON", coupon, "Hero section voucher coupon code");

        // Dynamically validate and register/enable Coupon Code in the system
        if (coupon != null && !coupon.trim().isEmpty()) {
            String cleanCoupon = coupon.trim().toUpperCase();
            
            // Try to find if coupon already exists
            couponRepository.findByCodeIgnoreCase(cleanCoupon).ifPresentOrElse(
                existing -> {
                    if (!existing.getIsActive()) {
                        existing.setIsActive(true);
                        couponRepository.save(existing);
                        log.info("Activated existing matching coupon code: {}", cleanCoupon);
                    }
                },
                () -> {
                    // Extract numerical discount suffix (e.g. GOPIGANJ10 -> 10%, MADHUR25 -> 25%)
                    BigDecimal discount = new BigDecimal("10.00");
                    Matcher codeNumMatcher = Pattern.compile("\\d+$").matcher(cleanCoupon);
                    if (codeNumMatcher.find()) {
                        try {
                            discount = new BigDecimal(codeNumMatcher.group());
                        } catch (Exception ignored) {}
                    } else {
                        // Scan subtitle for a percent amount (e.g. "15% off")
                        Matcher subtitleMatcher = Pattern.compile("(\\d+)\\s*%").matcher(subtitle);
                        if (subtitleMatcher.find()) {
                            try {
                                discount = new BigDecimal(subtitleMatcher.group(1));
                            } catch (Exception ignored) {}
                        }
                    }

                    Coupon newCoupon = Coupon.builder()
                            .code(cleanCoupon)
                            .discountPercentage(discount)
                            .minOrderValue(BigDecimal.ZERO)
                            .isActive(true)
                            .maxUsagePerUser(5)
                            .build();
                    couponRepository.save(newCoupon);
                    log.info("Automatically generated and saved missing coupon code: {} with discount: {}%", cleanCoupon, discount);
                }
            );
        }

        auditLogService.log("UPDATE_HERO_SETTINGS", null, "Hero config updated. Type: " + type + ", Coupon: " + coupon);
        log.info("Hero configurations updated by admin to type: {}", type);

        return getPublicHeroSettings();
    }
}
