package com.madhurgram.productservice.logistics.service;

import com.madhurgram.productservice.order.entity.Order;

public interface LogisticsService {
    /**
     * Schedules courier pickup manifests for a validated order.
     */
    void scheduleOrderPickup(Order order);
}
