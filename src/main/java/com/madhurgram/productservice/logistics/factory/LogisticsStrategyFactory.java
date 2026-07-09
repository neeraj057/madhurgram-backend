package com.madhurgram.productservice.logistics.factory;

import com.madhurgram.productservice.logistics.provider.LogisticsProvider;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LogisticsStrategyFactory {

    private final Map<String, LogisticsProvider> providers;

    public LogisticsStrategyFactory(List<LogisticsProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        p -> p.getProviderName().toUpperCase(),
                        Function.identity()
                ));
    }

    public LogisticsProvider getProvider(String providerName) {
        if (providerName == null) {
            throw new IllegalArgumentException("Logistics provider name cannot be null.");
        }
        LogisticsProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported logistics provider: " + providerName);
        }
        return provider;
    }
}
