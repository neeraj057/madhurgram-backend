package com.madhurgram.productservice.logistics.provider;

import com.madhurgram.productservice.order.entity.Order;

public interface LogisticsProvider {
    /**
     * Unique identifier matching the logistics provider.
     */
    String getProviderName();

    /**
     * Dispatches order manifest to courier APIs.
     */
    LogisticsShipmentResponse requestPickup(Order order);
}
