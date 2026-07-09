package com.madhurgram.productservice.logistics.provider.impl;

import com.madhurgram.productservice.logistics.provider.LogisticsProvider;
import com.madhurgram.productservice.logistics.provider.LogisticsShipmentResponse;
import com.madhurgram.productservice.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class DelhiveryLogisticsProvider implements LogisticsProvider {

    private static final Logger log = LoggerFactory.getLogger(DelhiveryLogisticsProvider.class);
    
    public static final String PROVIDER_NAME = "DELHIVERY";
    public static final String COURIER_NAME = "Delhivery Express";
    public static final String AWB_PREFIX = "AWB-DELHIVERY-";

    private final Random random = new Random();

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public LogisticsShipmentResponse requestPickup(Order order) {
        log.info("[{}] Sending pickup request for Order ID: {} (Customer: {})", 
                PROVIDER_NAME, order.getId(), order.getCustomerName());
        
        // Mock connection latency
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long awb = 1000000000L + (Math.abs(random.nextLong()) % 9000000000L);
        return new LogisticsShipmentResponse(AWB_PREFIX + awb, COURIER_NAME, true);
    }
}
