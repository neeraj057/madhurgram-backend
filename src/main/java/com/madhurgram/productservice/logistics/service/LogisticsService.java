package com.madhurgram.productservice.logistics.service;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Random;

@Service
public class LogisticsService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsService.class);
    private final OrderRepository orderRepository;
    private final OrderNotificationService orderNotificationService;
    private final Random random = new Random();

    public LogisticsService(OrderRepository orderRepository, OrderNotificationService orderNotificationService) {
        this.orderRepository = orderRepository;
        this.orderNotificationService = orderNotificationService;
    }

    @Transactional
    public void scheduleOrderPickup(Order order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot schedule pickup: Order is null.");
            return;
        }

        log.info("Initiating automated Delhivery Express pickup dispatcher for Order ID: {}", order.getId());

        // 1. Simulate API connection latency
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. Generate mock Air Waybill (AWB) tracking number
        long awbSuffix = 1000000000L + (Math.abs(random.nextLong()) % 9000000000L);
        String trackingNumber = "AWB-DELHIVERY-" + awbSuffix;
        String courier = "Delhivery Express";

        log.info("Courier manifest generated successfully. Carrier: {}, AWB: {}", courier, trackingNumber);

        // 3. Update Order details and transition status to SHIPPED
        order.setTrackingNumber(trackingNumber);
        order.setCourierName(courier);
        order.setOrderStatus(OrderStatus.SHIPPED);

        Order saved = orderRepository.save(order);
        log.info("Order ID: {} has been auto-transitioned to SHIPPED.", order.getId());

        // 4. Trigger dispatch WhatsApp notification (sends customer tracking details)
        try {
            orderNotificationService.sendOrderStatusNotification(saved, OrderStatus.SHIPPED);
        } catch (Exception e) {
            log.error("Failed to trigger shipped notification for order ID: {}. Error: {}", order.getId(), e.getMessage());
        }
    }
}
