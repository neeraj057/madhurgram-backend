package com.madhurgram.productservice.logistics.service.impl;

import com.madhurgram.productservice.logistics.factory.LogisticsStrategyFactory;
import com.madhurgram.productservice.logistics.provider.LogisticsProvider;
import com.madhurgram.productservice.logistics.provider.LogisticsShipmentResponse;
import com.madhurgram.productservice.logistics.service.LogisticsService;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for dynamically dispatching pick-up requests and tracking courier waybills.
 */
@Slf4j
@Service
public class LogisticsServiceImpl implements LogisticsService {

    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;
    private final LogisticsStrategyFactory logisticsStrategyFactory;

    @Value("${madhurgram.logistics.provider:DELHIVERY}")
    private String activeProvider;

    /**
     * Constructor injection for LogisticsServiceImpl.
     *
     * @param orderRepository          order repository
     * @param orderNotificationService order notifications client
     * @param logisticsStrategyFactory carrier strategy factory
     */
    public LogisticsServiceImpl(
            OrderRepository orderRepository,
            OrderNotificationService orderNotificationService,
            LogisticsStrategyFactory logisticsStrategyFactory) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
        this.logisticsStrategyFactory = logisticsStrategyFactory;
    }

    /**
     * Schedules carrier pickups dynamically using selected LogisticsProvider strategies.
     * Updates order waybills and triggers customer alerts.
     *
     * @param order database order model to ship
     */
    @Override
    @Transactional
    public void scheduleOrderPickup(Order order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot schedule pickup: Order or Order ID is null.");
            throw new IllegalArgumentException("Order and Order ID must not be null.");
        }

        log.info("Resolving logistics provider for: '{}' and Order ID: {}", activeProvider, order.getId());
        
        try {
            // 1. Resolve carrier strategy dynamically
            LogisticsProvider provider = logisticsStrategyFactory.getProvider(activeProvider.trim().toUpperCase());
            
            // 2. Call carrier pickup booking endpoint
            LogisticsShipmentResponse response = provider.requestPickup(order);
            
            if (response.success()) {
                log.info("Pickup scheduled successfully via '{}'. Waybill tracking ref: '{}'", 
                        response.courierName(), response.trackingNumber());
                
                // 3. Persist courier metadata and update status to SHIPPED
                order.setTrackingNumber(response.trackingNumber());
                order.setCourierName(response.courierName());
                order.setOrderStatus(OrderStatus.SHIPPED);
                
                Order saved = orderRepository.save(order);
                log.info("Order ID: {} status updated to SHIPPED in database", order.getId());

                // 4. Dispatch shipped notification alert to customer
                try {
                    orderNotificationService.sendOrderStatusNotification(saved, OrderStatus.SHIPPED);
                } catch (Exception e) {
                    log.error("Failed to send shipping notification for Order ID: {}. Error: {}", order.getId(), e.getMessage());
                }
            } else {
                log.error("Logistics provider '{}' failed to process pickup request for Order ID: {}", activeProvider, order.getId());
            }
        } catch (Exception e) {
            log.error("Failed to execute pickup scheduler commands for Order ID: {}. Error: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to schedule logistics pickup.", e);
        }
    }
}
