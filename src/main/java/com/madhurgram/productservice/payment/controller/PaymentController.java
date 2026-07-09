package com.madhurgram.productservice.payment.controller;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderService;
import com.madhurgram.productservice.payment.factory.PaymentStrategyFactory;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Value("${madhurgram.payment.provider:STRIPE}")
    private String activeProvider;

    public PaymentController(
            OrderRepository orderRepository, 
            OrderService orderService, 
            PaymentStrategyFactory paymentStrategyFactory) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.paymentStrategyFactory = paymentStrategyFactory;
    }

    @PostMapping("/webhook")
    @org.springframework.cache.annotation.CacheEvict(value = "analytics", allEntries = true)
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 20, windowSeconds = 60)
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Received payment gateway webhook event: {} via active provider: {}", payload, activeProvider);
        try {
            // 1. Resolve strategy dynamically using Strategy Factory (SOLID - Open/Closed)
            PaymentProcessor processor = paymentStrategyFactory.getProcessor(activeProvider);
            
            // 2. Delegate webhook parsing and security checksum validation to specific provider
            boolean verified = processor.processWebhook(payload);
            if (!verified) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook signature or payload verification failed."));
            }

            String eventType = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            
            if (data == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing data payload."));
            }

            Number orderIdNum = (Number) data.get("orderId");
            if (orderIdNum == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing orderId in data payload."));
            }
            Long orderId = orderIdNum.longValue();
            String transactionId = (String) data.get("transactionId");

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

            if ("payment_intent.succeeded".equalsIgnoreCase(eventType)) {
                log.info("Processing successful payment for Order ID: {} (Transaction: {})", orderId, transactionId);
                order.setPaymentStatus("COMPLETED");
                order.setPaymentTransactionId(transactionId);
                orderRepository.save(order);

                // Enforce lifecycle update: Transition PENDING to CONFIRMED
                orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);
                return ResponseEntity.ok(Map.of("message", "Payment processed successfully. Order CONFIRMED."));

            } else if ("payment_intent.failed".equalsIgnoreCase(eventType)) {
                log.warn("Processing failed payment for Order ID: {}", orderId);
                order.setPaymentStatus("FAILED");
                orderRepository.save(order);

                // Transition order to CANCELLED (which automatically releases stock)
                orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                return ResponseEntity.ok(Map.of("message", "Payment failed processed. Order CANCELLED."));
            }

            return ResponseEntity.ok(Map.of("message", "Event ignored. Type: " + eventType));
        } catch (Exception e) {
            log.error("Error processing payment webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
