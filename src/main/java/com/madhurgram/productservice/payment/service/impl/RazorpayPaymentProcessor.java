package com.madhurgram.productservice.payment.service.impl;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Mock payment processor implementation for Razorpay gateway.
 */
@Slf4j
@Service
public class RazorpayPaymentProcessor implements PaymentProcessor {

    private static final String PROVIDER_NAME = "RAZORPAY";
    private static final String SESSION_URL_TEMPLATE = "https://api.razorpay.com/v1/checkout/mock_order_%d";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public String createPaymentSession(Order order) {
        if (order == null || order.getId() == null) {
            log.warn("Cannot create Razorpay session: Order or Order ID is null.");
            throw new IllegalArgumentException("Order details must not be null.");
        }

        log.info("[RAZORPAY] Generating mock Razorpay Order ID for checkout session. Order ID: {}", 
                order.getId());
        return String.format(SESSION_URL_TEMPLATE, order.getId());
    }

    @Override
    public boolean processWebhook(Map<String, Object> payload) {
        log.info("[RAZORPAY] Verifying Razorpay payment signatures and attributes: {}", payload);
        return true;
    }
}
