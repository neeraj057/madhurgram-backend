package com.madhurgram.productservice.logistics.service.impl;

import com.madhurgram.productservice.logistics.factory.LogisticsStrategyFactory;
import com.madhurgram.productservice.logistics.provider.LogisticsProvider;
import com.madhurgram.productservice.logistics.provider.LogisticsShipmentResponse;
import com.madhurgram.productservice.logistics.service.LogisticsService;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogisticsServiceImpl implements LogisticsService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsServiceImpl.class);
    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;
    private final LogisticsStrategyFactory logisticsStrategyFactory;

    @Value("${madhurgram.logistics.provider:DELHIVERY}")
    private String activeProvider;

    public LogisticsServiceImpl(
            OrderRepository orderRepository,
            OrderNotificationService orderNotificationService,
            LogisticsStrategyFactory logisticsStrategyFactory) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
        this.logisticsStrategyFactory = logisticsStrategyFactory;
    }

    @Override
    @Transactional
    public void scheduleOrderPickup(Order order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot schedule pickup: Order is null.");
            return;
        }

        log.info("Resolving logistics provider for: {}", activeProvider);
        
        try {
            // 1. Resolve carrier strategy dynamically
            LogisticsProvider provider = logisticsStrategyFactory.getProvider(activeProvider);
            
            // 2. Call abstract carrier dispatch logic
            LogisticsShipmentResponse response = provider.requestPickup(order);
            
            if (response.success()) {
                log.info("Pickup scheduled successfully via {}. Waybill: {}", 
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
                    log.error("Failed to send shipping notification: {}", e.getMessage());
                }
            } else {
                log.error("Logistics provider failed to process pickup request.");
            }
        } catch (Exception e) {
            log.error("Failed to run pickup dispatcher: {}", e.getMessage(), e);
        }
    }
}
