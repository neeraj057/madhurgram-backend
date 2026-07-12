package com.madhurgram.productservice.cart.service.impl;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.dto.CartUpdateRequest;
import com.madhurgram.productservice.cart.entity.AbandonedCart;
import com.madhurgram.productservice.cart.mapper.CartMapper;
import com.madhurgram.productservice.cart.repository.AbandonedCartRepository;
import com.madhurgram.productservice.cart.service.AbandonedCartService;
import com.madhurgram.productservice.cart.service.TwilioService;
import com.madhurgram.productservice.common.entity.SystemSetting;
import com.madhurgram.productservice.common.repository.SystemSettingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing abandoned cart sessions, recovery, 
 * and automated marketing WhatsApp follow-up reminder cycles.
 */
@Slf4j
@Service
public class AbandonedCartServiceImpl implements AbandonedCartService {

    private static final String AUTO_RECOVERY_KEY = "AUTO_RECOVERY_ENABLED";
    private static final String DEFAULT_RECOVERY_TEMPLATE = "{greeting} आपने MadhurGram पर शुद्ध, विलेज-क्राफ़्टेड Ghee & तेल्स कार्ट में छोड़े थे। 🌾\n\n" +
            "ऑर्डर पूरा करने के लिए आपकी कार्ट यहाँ सुरक्षित है। आपके लिए स्पेशल 5% डिस्काउंट कूपन तैयार है!\n\n" +
            "Complete your order here: {deepLink}\n\n" +
            "धन्यवाद, टीम MadhurGram 💛";

    private final AbandonedCartRepository repository;
    private final SystemSettingRepository systemSettingRepository;
    private final TwilioService twilioService;
    private final CartMapper cartMapper;

    @org.springframework.beans.factory.annotation.Value("${madhurgram.cart.retention-hours:48}")
    private int retentionHours;

    @org.springframework.beans.factory.annotation.Value("${madhurgram.app.domain-url:http://localhost:3000}")
    private String domainUrl;

    /**
     * Constructor injection for AbandonedCartServiceImpl.
     *
     * @param repository              cart database repository
     * @param systemSettingRepository configuration settings repository
     * @param twilioService           SMS/WhatsApp messaging client
     * @param cartMapper              cart data mapper
     */
    public AbandonedCartServiceImpl(
            AbandonedCartRepository repository,
            SystemSettingRepository systemSettingRepository,
            TwilioService twilioService,
            CartMapper cartMapper) {
        this.repository = repository;
        this.systemSettingRepository = systemSettingRepository;
        this.twilioService = twilioService;
        this.cartMapper = cartMapper;
    }

    /**
     * Updates/syncs the current state of items inside a customer's active shopping cart.
     *
     * @param request cart details payload containing phone, name, and items
     * @return the saved cart response wrapper
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public AbandonedCartResponse updateCart(CartUpdateRequest request) {
        log.info("Updating cart state for phone number: {}", request.getPhoneNumber());
        
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            log.warn("Cart update aborted: phone number parameter is blank");
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        Optional<AbandonedCart> existingOpt = repository.findByPhoneNumberAndIsRecoveredFalse(request.getPhoneNumber().trim());
        AbandonedCart cart;

        if (existingOpt.isPresent()) {
            cart = existingOpt.get();
            log.info("Existing active cart found (ID: {}). Merging newer items and amount.", cart.getId());
            cart.setCartItemsJson(request.getCartItemsJson());
            cart.setTotalAmount(request.getTotalAmount());
            if (request.getCustomerName() != null && !request.getCustomerName().trim().isEmpty()) {
                cart.setCustomerName(request.getCustomerName().trim());
            }
            cart.setLastUpdated(LocalDateTime.now());
        } else {
            log.info("Creating a new abandoned cart session for phone: '{}'", request.getPhoneNumber().trim());
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
        log.info("Successfully updated cart session ID: {}", saved.getId());
        return cartMapper.toResponse(saved);
    }

    /**
     * Retrieves the unrecovered cart associated with a phone number.
     *
     * @param phoneNumber customer phone number
     * @return optional containing the recovered cart details if found
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AbandonedCartResponse> getCartToRecover(String phoneNumber) {
        log.info("Request: recover cart for phone number: '{}'", phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Cart recovery lookup skipped: phone parameter is blank");
            return Optional.empty();
        }
        
        return repository.findByPhoneNumberAndIsRecoveredFalse(phoneNumber.trim())
                .map(cartMapper::toResponse);
    }

    /**
     * Lists active abandoned carts that haven't been updated since a cutoff.
     *
     * @param minutesAgo inactive limit threshold
     * @return list of matching abandoned cart responses
     */
    @Override
    @Transactional(readOnly = true)
    public List<AbandonedCartResponse> getAbandonedCarts(int minutesAgo) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesAgo);
        log.info("Retrieving unrecovered carts inactive since before: {}", cutoff);
        List<AbandonedCart> carts = repository.findByIsRecoveredFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(cutoff);
        return carts.stream()
                .map(cartMapper::toResponse)
                .toList();
    }

    /**
     * Flags a cart as recovered upon order completion.
     *
     * @param phoneNumber customer phone number
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void markAsRecovered(String phoneNumber) {
        log.info("Marking cart as recovered for phone number: '{}'", phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Cannot mark cart as recovered: phone parameter is empty");
            return;
        }

        Optional<AbandonedCart> cartOpt = repository.findByPhoneNumberAndIsRecoveredFalse(phoneNumber.trim());
        if (cartOpt.isPresent()) {
            AbandonedCart cart = cartOpt.get();
            cart.setRecovered(true);
            repository.save(cart);
            log.info("Successfully marked cart ID: {} as recovered for phone: '{}'", cart.getId(), phoneNumber);
        } else {
            log.info("No active unrecovered cart found to flag for phone: '{}'", phoneNumber);
        }
    }

    /**
     * Checks if automated recovery reminder engine is active.
     *
     * @return true if autopilot recovery is enabled
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isAutoRecoveryEnabled() {
        return systemSettingRepository.findById(AUTO_RECOVERY_KEY)
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);
    }

    /**
     * Toggles the automated recovery autopilot status.
     *
     * @param enabled status state
     */
    @Override
    @Transactional
    public void setAutoRecoveryEnabled(boolean enabled) {
        log.info("Toggling automated autopilot recovery status to: {}", enabled);
        SystemSetting setting = systemSettingRepository.findById(AUTO_RECOVERY_KEY)
                .orElse(SystemSetting.builder()
                        .settingKey(AUTO_RECOVERY_KEY)
                        .description("Master toggle for automated WhatsApp cart recovery reminders")
                        .build());
        setting.setSettingValue(String.valueOf(enabled));
        systemSettingRepository.save(setting);
        log.info("Autopilot recovery status successfully saved as: {}", enabled);
    }

    /**
     * Executes automated WhatsApp notifications to prompt checkouts.
     */
    @Override
    @Transactional
    public void sendAutomatedReminders() {
        if (!isAutoRecoveryEnabled()) {
            log.info("Automated Cart Recovery reminder engine is currently disabled. Skipping cron run.");
            return;
        }

        log.info("Running automated cart recovery worker cycle...");
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<AbandonedCart> pendingReminders = repository
                .findByIsRecoveredFalseAndReminderSentFalseAndLastUpdatedBeforeOrderByLastUpdatedDesc(cutoff);

        log.info("Found {} pending abandoned cart(s) for auto-reminder dispatch.", pendingReminders.size());

        for (AbandonedCart cart : pendingReminders) {
            try {
                log.info("Dispatching WhatsApp reminder for cart ID: {}, phone: '{}'", cart.getId(), cart.getPhoneNumber());
                
                String greeting = cart.getCustomerName() != null ? "नमस्ते " + cart.getCustomerName() + "!" : "नमस्ते!";
                String deepLink = domainUrl + "/?recoverCart=" + cart.getPhoneNumber();
                
                String template = getRecoveryMessageTemplate();
                String message = template
                        .replace("{greeting}", greeting)
                        .replace("{deepLink}", deepLink);

                twilioService.sendWhatsAppMessage(cart.getPhoneNumber(), message);

                cart.setReminderSent(true);
                cart.setReminderSentAt(LocalDateTime.now());
                repository.save(cart);
                
                log.info("Successfully dispatched auto-reminder and updated cart ID: {}", cart.getId());
            } catch (Exception e) {
                log.error("Failed to dispatch automated reminder for cart ID: {}, phone: '{}'. Error: {}", 
                        cart.getId(), cart.getPhoneNumber(), e.getMessage(), e);
            }
        }
    }

    /**
     * Purges expired unrecovered abandoned carts from database.
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void purgeExpiredCarts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        log.info("Executing cleanup purge for carts older than: {} hours (Cutoff: {})", retentionHours, cutoff);
        try {
            int deletedCount = repository.deleteExpiredCarts(cutoff);
            if (deletedCount > 0) {
                log.info("Successfully purged {} expired cart(s) from database", deletedCount);
            } else {
                log.debug("No expired carts found to purge.");
            }
        } catch (Exception e) {
            log.error("Failed to execute expired carts purge query: {}", e.getMessage(), e);
        }
    }

    /**
     * Manually deletes an abandoned cart by its ID.
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void deleteAbandonedCart(Long id) {
        log.info("Deleting abandoned cart by ID: {}", id);
        
        if (id == null) {
            log.warn("Cart deletion aborted: ID parameter is null");
            throw new IllegalArgumentException("Cart ID cannot be null.");
        }

        try {
            repository.deleteById(id);
            log.info("Successfully deleted abandoned cart ID: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete abandoned cart ID: {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete abandoned cart.", e);
        }
    }

    private String getRecoveryMessageTemplate() {
        return systemSettingRepository.findById("WHATSAPP_RECOVERY_TEMPLATE")
                .map(com.madhurgram.productservice.common.entity.SystemSetting::getSettingValue)
                .orElse(DEFAULT_RECOVERY_TEMPLATE);
    }
}
