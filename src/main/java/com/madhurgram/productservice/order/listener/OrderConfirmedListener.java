package com.madhurgram.productservice.order.listener;

import com.madhurgram.productservice.order.event.OrderConfirmedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that handles order confirmation events after successful database commits.
 */
@Slf4j
@Component
public class OrderConfirmedListener {

    /**
     * Responds asynchronously to order confirmation events.
     * Currently configured to await manual admin dispatch triggers.
     *
     * @param event order confirmation details event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
        if (event == null || event.getOrder() == null) {
            log.warn("Discarded null OrderConfirmedEvent.");
            return;
        }

        log.info("OrderConfirmedEvent received for Order ID: {} - Auto-logistics booking disabled to wait for manual dispatch by Admin.", 
                event.getOrder().getId());
        // Automatic logistics dispatch is disabled to support standard warehouse packing confirmation.
    }
}
