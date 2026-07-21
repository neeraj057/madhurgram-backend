package com.madhurgram.productservice.payment.service;

import com.madhurgram.productservice.order.entity.Order;
import java.util.Map;

public interface PaymentProcessor {
    /**
     * Unique identifier matching strategy name.
     */
    String getProviderName();

    /**
     * Generates session URL links for clients redirecting.
     */
    String createPaymentSession(Order order);

    /**
     * Processes transaction payload and updates payment state.
     */
    boolean processWebhook(Map<String, Object> payload);

    /**
     * Verifies the cryptographic HMAC signature returned by client checkout SDK.
     */
    boolean verifyPaymentSignature(Map<String, String> attributes);
}
