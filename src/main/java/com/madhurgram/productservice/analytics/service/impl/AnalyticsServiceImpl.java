package com.madhurgram.productservice.analytics.service.impl;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.analytics.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    // Clean Constructor Injection
    public AnalyticsServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true) // 🔒 Read-Only transaction for performance optimization
    public AdminAnalyticsDTO getDailyDashboardMetrics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        return AdminAnalyticsDTO.builder()
                .todayRevenue(orderRepository.getTodayRevenue(startOfDay, endOfDay))
                .todayOrderCount(orderRepository.getTodayOrderCount(startOfDay, endOfDay))
                .pendingOrderCount(orderRepository.getPendingOrderCount())
                .lowStockProductCount(productRepository.getLowStockCount())
                .build();
    }
}