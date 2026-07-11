package com.madhurgram.productservice.order.listener;

import com.madhurgram.productservice.order.event.OrderConfirmedEvent;
import com.madhurgram.productservice.logistics.service.LogisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderConfirmedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmedListener.class);
    private final LogisticsService logisticsService;

    public OrderConfirmedListener(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
        log.info("OrderConfirmedEvent received for Order ID: {} - Auto-logistics booking disabled to wait for manual dispatch by Admin.", 
                event.getOrder().getId());
        // Automatic logistics dispatch is disabled to support standard warehouse packing confirmation.
    }
}
