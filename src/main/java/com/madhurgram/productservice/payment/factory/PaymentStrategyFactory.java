package com.madhurgram.productservice.payment.factory;

import com.madhurgram.productservice.payment.service.PaymentProcessor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyFactory {

    private final Map<String, PaymentProcessor> processors;

    public PaymentStrategyFactory(List<PaymentProcessor> processorList) {
        this.processors = processorList.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toUpperCase(),
                        Function.identity()
                ));
    }

    public PaymentProcessor getProcessor(String providerName) {
        if (providerName == null) {
            throw new IllegalArgumentException("Payment provider name cannot be null.");
        }
        PaymentProcessor processor = processors.get(providerName.toUpperCase());
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + providerName);
        }
        return processor;
    }
}
