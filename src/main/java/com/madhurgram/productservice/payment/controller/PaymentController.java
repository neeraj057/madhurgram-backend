package com.madhurgram.productservice.payment.controller;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.service.OrderService;
import com.madhurgram.productservice.payment.factory.PaymentStrategyFactory;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling payment transactions, integration gateways, and external webhooks.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments Gateway", description = "Endpoints for processing checkout payments and handling gateway webhooks")
public class PaymentController {

    private static final String EVENT_SUCCEEDED = "payment_intent.succeeded";
    private static final String EVENT_FAILED = "payment_intent.failed";

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final OrderService orderService;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Value("${madhurgram.payment.provider:STRIPE}")
    private String activeProvider;

    /**
     * Constructor injection for PaymentController.
     * Decoupled from direct repository access to follow strict MVC boundaries.
     *
     * @param orderService           order business services
     * @param paymentStrategyFactory strategy factory resolved by provider type
     */
    public PaymentController(
            OrderService orderService, 
            PaymentStrategyFactory paymentStrategyFactory) {
        this.orderService = orderService;
        this.paymentStrategyFactory = paymentStrategyFactory;
    }

    /**
     * Receives and validates asynchronous webhooks sent by the active payment provider (e.g. Stripe, Razorpay).
     *
     * @param payload external webhook payload Map containing event type and customer tracking fields
     * @return updated order details DTO JSON response
     */
    @PostMapping("/webhook")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 20, windowSeconds = 60)
    @Operation(summary = "Handle gateway payment webhook", description = "Asynchronously processes incoming gateway webhooks to capture successful payments or handle checkout failures.")
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Request: payment webhook received. Provider='{}'", activeProvider);
        try {
            // 1. Resolve strategy dynamically using Strategy Factory (SOLID - Open/Closed)
            PaymentProcessor processor = paymentStrategyFactory.getProcessor(activeProvider);
            
            // 2. Delegate webhook parsing and security checksum validation to specific provider
            boolean verified = processor.processWebhook(payload);
            if (!verified) {
                log.warn("Payment webhook verification failed: invalid signature / payload");
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid webhook signature or payload verification failed."));
            }

            String eventType = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            
            if (data == null) {
                log.warn("Payment webhook aborted: missing data payload");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing data payload."));
            }

            Number orderIdNum = (Number) data.get("orderId");
            if (orderIdNum == null) {
                log.warn("Payment webhook aborted: missing orderId in payload data");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing orderId in data payload."));
            }
            Long orderId = orderIdNum.longValue();
            String transactionId = (String) data.get("transactionId");

            if (EVENT_SUCCEEDED.equalsIgnoreCase(eventType)) {
                log.info("Processing successful payment for Order ID: {} (Transaction: {})", orderId, transactionId);
                OrderResponseDTO response = orderService.updateOrderPaymentStatus(orderId, STATUS_COMPLETED, transactionId);
                return ResponseEntity.ok(response);

            } else if (EVENT_FAILED.equalsIgnoreCase(eventType)) {
                log.warn("Processing failed payment for Order ID: {}", orderId);
                OrderResponseDTO response = orderService.updateOrderPaymentStatus(orderId, STATUS_FAILED, transactionId);
                return ResponseEntity.ok(response);
            }

            log.info("Ignored payment webhook event type: '{}'", eventType);
            return ResponseEntity.ok(Map.of("message", "Event ignored. Type: " + eventType));
        } catch (Exception e) {
            log.error("Error occurred while processing payment webhook", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
