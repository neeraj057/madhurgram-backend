package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.repository.OrderRepository;
import com.madhurgram.productservice.repository.ProductRepository;
import com.madhurgram.productservice.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return AdminAnalyticsDTO.builder()
                .todayRevenue(orderRepository.getTodayRevenue())
                .todayOrderCount(orderRepository.getTodayOrderCount())
                .pendingOrderCount(orderRepository.getPendingOrderCount())
                .lowStockProductCount(productRepository.getLowStockCount())
                .build();
    }
}