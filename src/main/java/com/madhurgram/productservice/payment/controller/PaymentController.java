package com.madhurgram.productservice.payment.controller;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.service.OrderService;
import com.madhurgram.productservice.payment.factory.PaymentStrategyFactory;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling payment transactions, integration gateways, and external webhooks.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
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
     * Generates an interactive payment gateway checkout session for an unpaid order.
     *
     * @param orderId ID of the order to generate session for
     * @return map containing the generated redirect session URL
     */
    @PostMapping("/create-session/{orderId}")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 10, windowSeconds = 60)
    @Operation(summary = "Create checkout session", description = "Generates a payment gateway redirect session link for the active provider.")
    public ResponseEntity<?> createPaymentSession(@PathVariable Long orderId) {
        log.info("Request: create payment session for Order ID: {}, provider='{}'", orderId, activeProvider);
        try {
            OrderResponseDTO orderDto = orderService.getOrderDetails(orderId);
            if (orderDto == null) {
                return ResponseEntity.notFound().build();
            }

            Order dummyOrder = Order.builder()
                    .id(orderDto.getId())
                    .totalAmount(orderDto.getTotalAmount())
                    .build();

            PaymentProcessor processor = paymentStrategyFactory.getProcessor(activeProvider);
            String sessionUrl = processor.createPaymentSession(dummyOrder);

            log.info("Payment session created successfully for Order ID: {}", orderId);
            return ResponseEntity.ok(Map.of("sessionUrl", sessionUrl, "provider", activeProvider));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment session creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create payment session for Order ID: {}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to initialize payment session."));
        }
    }

    /**
     * Verifies client-side payment completion signatures (e.g. Razorpay HMAC verification).
     *
     * @param attributes map containing razorpay_order_id, razorpay_payment_id, razorpay_signature, and orderId
     * @return response indicating whether signature is valid and order status updated
     */
    @PostMapping("/verify-signature")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 10, windowSeconds = 60)
    @Operation(summary = "Verify checkout payment signature", description = "Cryptographically verifies payment signatures returned by the gateway checkout popup.")
    public ResponseEntity<?> verifyPaymentSignature(@RequestBody Map<String, String> attributes) {
        log.info("Request: verify payment signature. Provider='{}'", activeProvider);
        try {
            PaymentProcessor processor = paymentStrategyFactory.getProcessor(activeProvider);
            boolean verified = processor.verifyPaymentSignature(attributes);

            if (!verified) {
                log.warn("Payment signature verification failed for provider '{}'", activeProvider);
                return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Invalid payment signature."));
            }

            String orderIdStr = attributes.get("orderId");
            String paymentId = attributes.get("razorpay_payment_id");

            if (orderIdStr != null) {
                Long orderId = Long.parseLong(orderIdStr);
                log.info("Updating Order #{} to COMPLETED following signature verification.", orderId);
                OrderResponseDTO response = orderService.updateOrderPaymentStatus(orderId, STATUS_COMPLETED, paymentId);
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.ok(Map.of("verified", true, "message", "Payment signature verified successfully."));
        } catch (Exception e) {
            log.error("Failed to verify payment signature", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
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
        if (payload == null || payload.isEmpty()) {
            log.warn("Payment webhook aborted: payload is null or empty");
            return ResponseEntity.badRequest().body(Map.of("error", "Webhook payload cannot be empty."));
        }

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
            if (eventType == null) {
                log.warn("Payment webhook aborted: missing 'type' parameter in payload");
                return ResponseEntity.badRequest().body(Map.of("error", "Missing 'type' in webhook payload."));
            }

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
