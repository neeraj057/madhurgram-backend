package com.madhurgram.productservice.payment.service.impl;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class StripePaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProcessor.class);

    @Override
    public String getProviderName() {
        return "STRIPE";
    }

    @Override
    public String createPaymentSession(Order order) {
        log.info("[STRIPE] Creating Stripe checkout session for order ID: {} (Amount: {})", 
                order.getId(), order.getTotalAmount());
        return "https://checkout.stripe.com/pay/mock_session_" + order.getId();
    }

    @Override
    public boolean processWebhook(Map<String, Object> payload) {
        log.info("[STRIPE] Parsing webhook event: {}", payload);
        // Returns true if verification succeeded
        return true; 
    }
}
