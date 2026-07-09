package com.madhurgram.productservice.cart.service.impl;

import com.madhurgram.productservice.cart.dto.CartUpdateRequest;
import com.madhurgram.productservice.cart.entity.AbandonedCart;
import com.madhurgram.productservice.cart.repository.AbandonedCartRepository;
import com.madhurgram.productservice.cart.service.AbandonedCartService;
import com.madhurgram.productservice.cart.service.TwilioService;
import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AbandonedCartServiceImpl implements AbandonedCartService {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartServiceImpl.class);
    private static final String AUTO_RECOVERY_KEY = "AUTO_RECOVERY_ENABLED";

    private final AbandonedCartRepository repository;
    private final SystemSettingRepository systemSettingRepository;
    private final TwilioService twilioService;

    @org.springframework.beans.factory.annotation.Value("${madhurgram.cart.retention-hours:48}")
    private int retentionHours;

    public AbandonedCartServiceImpl(
            AbandonedCartRepository repository,
            SystemSettingRepository systemSettingRepository,
            TwilioService twilioService) {
        this.repository = repository;
        this.systemSettingRepository = systemSettingRepository;
        this.twilioService = twilioService;
    }

    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public AbandonedCart updateCart(CartUpdateRequest request) {
        log.info("Updating cart state for phone number: {}", request.getPhoneNumber());
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        Optional<AbandonedCart> existingOpt = repository.findByPhoneNumberAndIsRecoveredFalse(request.getPhoneNumber().trim());
        AbandonedCart cart;

        if (existingOpt.isPresent()) {
            cart = existingOpt.get();
            log.info("Existing unrecovered cart found (ID: {}). Updating items and total.", cart.getId());
            cart.setCartItemsJson(request.getCartItemsJson());
            cart.setTotalAmount(request.getTotalAmount());
            if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                cart.setCustomerName(request.getCustomerName().trim());
            }
            cart.setLastUpdated(LocalDateTime.now());
        } else {
            log.info("No active cart session found. Creating a new tracking manifest.");
            cart = AbandonedCart.builder()
                    .phoneNumber(request.getPhoneNumber().trim())
                    .customerName(request.getCustomerName() != null ? request.getCustomerName().trim() : null)
                    .cartItemsJson(request.getCartItemsJson())
                    .totalAmount(request.getTotalAmount())
                    .lastUpdated(LocalDateTime.now())
                    .isRecovered(false)
                    .build();
        }

        AbandonedCart saved = repository.save(cart);
        log.info("Saved cart tracking manifest with ID: {}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AbandonedCart> getCartToRecover(String phoneNumber) {
        log.info("Retrieving cart for recovery with phone number: {}", phoneNumber);
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.findByPhoneNumberAndIsRecoveredFalse(phoneNumber.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AbandonedCart> getAbandonedCarts(int minutesAgo) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesAgo);
        log.info("Fetching unrecovered carts inactive since before: {}", cutoff);
        return repository.findByIsRecoveredFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(cutoff);
    }

    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void markAsRecovered(String phoneNumber) {
        log.info("Attempting to mark cart as recovered for phone number: {}", phoneNumber);
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return;
        }

        Optional<AbandonedCart> cartOpt = repository.findByPhoneNumberAndIsRecoveredFalse(phoneNumber.trim());
        if (cartOpt.isPresent()) {
            AbandonedCart cart = cartOpt.get();
            cart.setRecovered(true);
            repository.save(cart);
            log.info("Successfully marked cart ID: {} as recovered for phone: {}", cart.getId(), phoneNumber);
        } else {
            log.info("No active unrecovered cart found to mark as recovered for phone: {}", phoneNumber);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAutoRecoveryEnabled() {
        return systemSettingRepository.findById(AUTO_RECOVERY_KEY)
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void setAutoRecoveryEnabled(boolean enabled) {
        log.info("Updating Auto-Pilot recovery engine status to: {}", enabled);
        SystemSetting setting = systemSettingRepository.findById(AUTO_RECOVERY_KEY)
                .orElse(SystemSetting.builder()
                        .settingKey(AUTO_RECOVERY_KEY)
                        .description("Master toggle for automated WhatsApp cart recovery reminders")
                        .build());
        setting.setSettingValue(String.valueOf(enabled));
        systemSettingRepository.save(setting);
    }

    @Override
    @Transactional
    public void sendAutomatedReminders() {
        if (!isAutoRecoveryEnabled()) {
            log.info("Automated Cart Recovery (Auto-Pilot) is disabled. Skipping dispatch.");
            return;
        }

        log.info("Running automated cart recovery worker...");
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<AbandonedCart> pendingReminders = repository
                .findByIsRecoveredFalseAndReminderSentFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(cutoff);

        log.info("Found {} pending abandoned carts for auto-reminder dispatch.", pendingReminders.size());

        for (AbandonedCart cart : pendingReminders) {
            try {
                log.info("Dispatching auto-reminder for cart ID: {}, phone: {}", cart.getId(), cart.getPhoneNumber());
                
                String greeting = cart.getCustomerName() != null ? "नमस्ते " + cart.getCustomerName() + "!" : "नमस्ते!";
                String domain = "http://localhost:3000";
                String deepLink = domain + "/?recoverCart=" + cart.getPhoneNumber();
                
                String message = greeting + " आपने MadhurGram पर शुद्ध, विलेज-क्राफ्टेड Ghee & तेल्स कार्ट में छोड़े थे। 🌾\n\n" +
                        "ऑर्डर पूरा करने के लिए आपकी कार्ट यहाँ सुरक्षित है। आपके लिए स्पेशल 5% डिस्काउंट कूपन तैयार है!\n\n" +
                        "Complete your order here: " + deepLink + "\n\n" +
                        "धन्यवाद, टीम MadhurGram 💛";

                twilioService.sendWhatsAppMessage(cart.getPhoneNumber(), message);

                cart.setReminderSent(true);
                cart.setReminderSentAt(LocalDateTime.now());
                repository.save(cart);
                
                log.info("Successfully dispatched automated reminder and updated cart ID: {}", cart.getId());
            } catch (Exception e) {
                log.error("Failed to dispatch automated reminder for cart ID: {}, phone: {}. Error: {}", 
                        cart.getId(), cart.getPhoneNumber(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void purgeExpiredCarts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        log.info("Starting purge execution for abandoned carts inactive since before: {} (Retention: {} hours)", 
                cutoff, retentionHours);
        try {
            int deletedCount = repository.deleteExpiredCarts(cutoff);
            if (deletedCount > 0) {
                log.info("Purged {} expired unrecovered abandoned carts from database.", deletedCount);
            } else {
                log.debug("No expired abandoned carts found to purge.");
            }
        } catch (Exception e) {
            log.error("Failed to execute purge query for expired abandoned carts: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void deleteAbandonedCart(Long id) {
        log.info("Executing delete command for abandoned cart ID: {}", id);
        try {
            repository.deleteById(id);
            log.info("Successfully deleted abandoned cart ID: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete abandoned cart ID: {}. Error: {}", id, e.getMessage(), e);
        }
    }
}
