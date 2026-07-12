package com.madhurgram.productservice.cart.scheduler;

import com.madhurgram.productservice.cart.service.AbandonedCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job task controller for triggering automated checkout recovery WhatsApp workflows 
 * and purging expired shopping carts from database indexes.
 */
@Slf4j
@Component
public class AbandonedCartScheduler {

    private final AbandonedCartService service;

    /**
     * Constructor injection for AbandonedCartScheduler.
     *
     * @param service abandoned cart management service
     */
    public AbandonedCartScheduler(AbandonedCartService service) {
        this.service = service;
    }

    /**
     * Runs periodic cart recovery dispatches and cleanup loops.
     * Interval configuration is dynamic and externalized to application properties.
     */
    @Scheduled(fixedRateString = "${madhurgram.cart.recovery-rate-ms:300000}")
    public void runAbandonedCartRecovery() {
        log.info("Scheduled task triggered: runAbandonedCartRecovery");
        
        try {
            log.info("Running scheduled cycle: send automated recovery reminders");
            service.sendAutomatedReminders();
        } catch (Exception e) {
            log.error("Error occurred during scheduled abandoned cart recovery reminder dispatch: {}", e.getMessage(), e);
        }
        
        try {
            log.info("Running scheduled cycle: purge expired unrecovered carts");
            service.purgeExpiredCarts();
        } catch (Exception e) {
            log.error("Error occurred during scheduled expired cart purging sequence: {}", e.getMessage(), e);
        }
    }
}
