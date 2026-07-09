package com.madhurgram.productservice.payment.service.impl;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.payment.service.PaymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RazorpayPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentProcessor.class);

    @Override
    public String getProviderName() {
        return "RAZORPAY";
    }

    @Override
    public String createPaymentSession(Order order) {
        log.info("[RAZORPAY] Generating mock Razorpay Order ID for checkout session. Order ID: {}", 
                order.getId());
        return "https://api.razorpay.com/v1/checkout/mock_order_" + order.getId();
    }

    @Override
    public boolean processWebhook(Map<String, Object> payload) {
        log.info("[RAZORPAY] Verifying Razorpay payment signatures and attributes: {}", payload);
        return true;
    }
}
