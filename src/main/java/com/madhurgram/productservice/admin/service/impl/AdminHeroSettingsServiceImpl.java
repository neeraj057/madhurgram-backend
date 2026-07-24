package com.madhurgram.productservice.admin.service.impl;

import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import com.madhurgram.productservice.audit.service.AuditLogService;
import com.madhurgram.productservice.coupon.repository.CouponRepository;
import com.madhurgram.productservice.coupon.entity.Coupon;
import com.madhurgram.productservice.admin.service.AdminHeroSettingsService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminHeroSettingsServiceImpl implements AdminHeroSettingsService {

    private final SystemSettingRepository repository;
    private final AuditLogService auditLogService;
    private final CouponRepository couponRepository;

    public AdminHeroSettingsServiceImpl(
            SystemSettingRepository repository, 
            AuditLogService auditLogService, 
            CouponRepository couponRepository) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        this.couponRepository = couponRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getHeroSettings() {
        Map<String, String> settings = new HashMap<>();
        settings.put("heroContentType", getSettingOrDefault("HERO_CONTENT_TYPE", "video"));
        settings.put("offerTitle", getSettingOrDefault("HERO_OFFER_TITLE", "Special Inaugural Swadeshi Offer"));
        settings.put("offerSubtitle", getSettingOrDefault("HERO_OFFER_SUBTITLE", "Use coupon code GOPIGANJ10 for 10% off on all organic cow ghee!"));
        settings.put("offerLink", getSettingOrDefault("HERO_OFFER_LINK", "/#products"));
        settings.put("offerCoupon", getSettingOrDefault("HERO_OFFER_COUPON", "GOPIGANJ10"));
        settings.put("customImageUrl", getSettingOrDefault("HERO_CUSTOM_IMAGE", ""));
        
        // Flash Sale Settings
        settings.put("flashSaleEnabled", getSettingOrDefault("FLASH_SALE_ENABLED", "false"));
        settings.put("flashSaleText", getSettingOrDefault("FLASH_SALE_TEXT", "Monsoon Wellness Sale: Get Flat 15% OFF on Premium Bilona Ghee"));
        settings.put("flashSaleEndTime", getSettingOrDefault("FLASH_SALE_END_TIME", ""));
        settings.put("flashSaleLink", getSettingOrDefault("FLASH_SALE_LINK", "/#products"));
        settings.put("flashSalePercentage", getSettingOrDefault("FLASH_SALE_PERCENTAGE", "15"));
        settings.put("flashSaleCategory", getSettingOrDefault("FLASH_SALE_CATEGORY", "shop-all"));
        
        return settings;
    }

    @Override
    @Transactional
    @CacheEvict(value = "promotions", allEntries = true)
    public Map<String, String> updateHeroSettings(Map<String, String> payload) {
        String type = payload.getOrDefault("heroContentType", "video");
        String title = payload.getOrDefault("offerTitle", "");
        String subtitle = payload.getOrDefault("offerSubtitle", "");
        String link = payload.getOrDefault("offerLink", "/#products");
        String coupon = payload.getOrDefault("offerCoupon", "");
        String customImageUrl = payload.getOrDefault("customImageUrl", "");

        saveSetting("HERO_CONTENT_TYPE", type, "Hero Section media source type (video, image, offer, custom)");
        saveSetting("HERO_OFFER_TITLE", title, "Hero section promo offer headline text");
        saveSetting("HERO_OFFER_SUBTITLE", subtitle, "Hero section promo details text");
        saveSetting("HERO_OFFER_LINK", link, "Hero section action redirection route link");
        saveSetting("HERO_OFFER_COUPON", coupon, "Hero section voucher coupon code");
        saveSetting("HERO_CUSTOM_IMAGE", customImageUrl, "Hero section custom banner image URL");

        // Flash Sale Settings
        String fsEnabled = payload.getOrDefault("flashSaleEnabled", "false");
        String fsText = payload.getOrDefault("flashSaleText", "Monsoon Wellness Sale: Get Flat 15% OFF on Premium Bilona Ghee");
        String fsEndTime = payload.getOrDefault("flashSaleEndTime", "");
        String fsLink = payload.getOrDefault("flashSaleLink", "/#products");
        String fsPercentage = payload.getOrDefault("flashSalePercentage", "15");
        String fsCategory = payload.getOrDefault("flashSaleCategory", "shop-all");
        
        saveSetting("FLASH_SALE_ENABLED", fsEnabled, "Is the top flash sale strip enabled?");
        saveSetting("FLASH_SALE_TEXT", fsText, "Text to display in the flash sale strip");
        saveSetting("FLASH_SALE_END_TIME", fsEndTime, "ISO 8601 end time for the flash sale timer");
        saveSetting("FLASH_SALE_LINK", fsLink, "URL to redirect on clicking claim now");
        saveSetting("FLASH_SALE_PERCENTAGE", fsPercentage, "Percentage discount to apply to target products");
        saveSetting("FLASH_SALE_CATEGORY", fsCategory, "Target category code for the flash sale discount");

        // Dynamically validate and register/enable Coupon Code in the system
        if (coupon != null && !coupon.trim().isEmpty()) {
            handleCouponRegistration(coupon, subtitle);
        }

        auditLogService.log("UPDATE_STOREFRONT_SETTINGS", null, "Storefront config updated. Type: " + type + ", FS Enabled: " + fsEnabled);
        log.info("Storefront configurations updated by admin to type: {}", type);

        return getHeroSettings();
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

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

    private void handleCouponRegistration(String coupon, String subtitle) {
        String cleanCoupon = coupon.trim().toUpperCase();
        
        couponRepository.findByCodeIgnoreCase(cleanCoupon).ifPresentOrElse(
            existing -> {
                if (!existing.getIsActive()) {
                    existing.setIsActive(true);
                    couponRepository.save(existing);
                    log.info("Activated existing matching coupon code: {}", cleanCoupon);
                }
            },
            () -> {
                BigDecimal discount = extractDiscount(cleanCoupon, subtitle);
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

    private BigDecimal extractDiscount(String coupon, String subtitle) {
        BigDecimal discount = new BigDecimal("10.00");
        Matcher codeNumMatcher = Pattern.compile("\\d+$").matcher(coupon);
        if (codeNumMatcher.find()) {
            try {
                discount = new BigDecimal(codeNumMatcher.group());
                return discount;
            } catch (Exception ignored) {}
        } 
        
        Matcher subtitleMatcher = Pattern.compile("(\\d+)\\s*%").matcher(subtitle);
        if (subtitleMatcher.find()) {
            try {
                discount = new BigDecimal(subtitleMatcher.group(1));
            } catch (Exception ignored) {}
        }
        return discount;
    }
}
