package com.madhurgram.productservice.analytics.service.impl;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.analytics.dto.DailyRevenueDTO;
import com.madhurgram.productservice.analytics.service.AnalyticsService;
import com.madhurgram.productservice.cart.repository.AbandonedCartRepository;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AbandonedCartRepository abandonedCartRepository;

    public AnalyticsServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AbandonedCartRepository abandonedCartRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.abandonedCartRepository = abandonedCartRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'daily'")
    public AdminAnalyticsDTO getDailyDashboardMetrics() {
        log.info("[CACHE MISS] Computing dynamic dashboard metrics for admin console...");

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 1. Calculate live conversion rate
        long totalCheckoutSessions = abandonedCartRepository.count();
        long recoveredCheckouts = abandonedCartRepository.countByIsRecoveredTrue();
        double conversionRate = totalCheckoutSessions > 0
                ? ((double) recoveredCheckouts / totalCheckoutSessions) * 100.0
                : 0.0;

        // 2. Fetch orders from last 7 days (including today)
        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        List<Order> recentOrders = orderRepository.findByOrderDateAfter(since);

        // Group by LocalDate in memory and sum amounts
        Map<LocalDate, BigDecimal> revenueMap = recentOrders.stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().toLocalDate(),
                        Collectors.mapping(Order::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // Create sequential 7-day data points (backwards from i=6 days ago to i=0 today)
        List<DailyRevenueDTO> revenueGraph = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            BigDecimal dailyRevenue = revenueMap.getOrDefault(date, BigDecimal.ZERO);
            revenueGraph.add(DailyRevenueDTO.builder()
                    .date(date.toString())
                    .revenue(dailyRevenue)
                    .build());
        }

        log.info("Dashboard stats: Conversion Rate = {}%, Graph Data Points = {}", conversionRate, revenueGraph.size());

        return AdminAnalyticsDTO.builder()
                .todayRevenue(orderRepository.getTodayRevenue(startOfDay, endOfDay))
                .todayOrderCount(orderRepository.getTodayOrderCount(startOfDay, endOfDay))
                .pendingOrderCount(orderRepository.getPendingOrderCount())
                .lowStockProductCount(productRepository.getLowStockCount())
                .conversionRate(conversionRate)
                .revenueGraph(revenueGraph)
                .build();
    }
}