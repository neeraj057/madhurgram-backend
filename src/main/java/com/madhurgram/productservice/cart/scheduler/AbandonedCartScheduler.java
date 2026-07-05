package com.madhurgram.productservice.cart.scheduler;

import com.madhurgram.productservice.cart.service.AbandonedCartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AbandonedCartScheduler {

    private static final Logger log = LoggerFactory.getLogger(AbandonedCartScheduler.class);
    private final AbandonedCartService service;

    public AbandonedCartScheduler(AbandonedCartService service) {
        this.service = service;
    }

    // Runs every 5 minutes (300,000 milliseconds)
    @Scheduled(fixedRate = 300000)
    public void runAbandonedCartRecovery() {
        log.info("Scheduled task triggered: runAbandonedCartRecovery");
        try {
            service.sendAutomatedReminders();
        } catch (Exception e) {
            log.error("Error occurred during scheduled abandoned cart recovery: {}", e.getMessage(), e);
        }
    }
}
