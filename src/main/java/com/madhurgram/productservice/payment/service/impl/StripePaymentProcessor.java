package com.madhurgram.productservice.payment.service.impl;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Mock payment processor implementation for Stripe gateway.
 */
@Slf4j
@Service
public class StripePaymentProcessor implements PaymentProcessor {

    private static final String PROVIDER_NAME = "STRIPE";
    private static final String SESSION_URL_TEMPLATE = "https://checkout.stripe.com/pay/mock_session_%d";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String createPaymentSession(Order order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot create Stripe session: Order or Order ID is null.");
            throw new IllegalArgumentException("Order details must not be null.");
        }

        log.info("[STRIPE] Creating Stripe checkout session for order ID: {} (Amount: {})", 
                order.getId(), order.getTotalAmount());
        return String.format(SESSION_URL_TEMPLATE, order.getId());
    }

    @Override
    public boolean processWebhook(Map<String, Object> payload) {
        log.info("[STRIPE] Parsing webhook event: {}", payload);
        // Returns true if verification succeeded
        return true; 
    }
}
