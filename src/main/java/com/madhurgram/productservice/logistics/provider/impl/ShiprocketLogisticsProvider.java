package com.madhurgram.productservice.logistics.provider.impl;

import com.madhurgram.productservice.logistics.provider.LogisticsProvider;
import com.madhurgram.productservice.logistics.provider.LogisticsShipmentResponse;
import com.madhurgram.productservice.order.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class ShiprocketLogisticsProvider implements LogisticsProvider {

    private static final Logger log = LoggerFactory.getLogger(ShiprocketLogisticsProvider.class);
    
    public static final String PROVIDER_NAME = "SHIPROCKET";
    public static final String COURIER_NAME = "Shiprocket Logistics";
    public static final String AWB_PREFIX = "AWB-SHIPROCKET-";

    private final Random random = new Random();

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public LogisticsShipmentResponse requestPickup(Order order) {
        log.info("[{}] Generating Shiprocket shipment order for order ID: {}", PROVIDER_NAME, order.getId());
        
        // Mock connection latency
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long awb = 2000000000L + (Math.abs(random.nextLong()) % 8000000000L);
        return new LogisticsShipmentResponse(AWB_PREFIX + awb, COURIER_NAME, true);
    }
}
