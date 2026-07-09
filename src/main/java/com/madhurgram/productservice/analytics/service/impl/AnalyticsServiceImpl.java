package com.madhurgram.productservice.analytics.service.impl;

import com.madhurgram.productservice.analytics.dto.AdminAnalyticsDTO;
import com.madhurgram.productservice.analytics.dto.DailyRevenueDTO;
import com.madhurgram.productservice.analytics.service.AnalyticsService;
import com.madhurgram.productservice.cart.repository.AbandonedCartRepository;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.madhurgram.productservice.analytics.dto.LowStockProductDTO;
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
    private final StringRedisTemplate redisTemplate;

    public AnalyticsServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            AbandonedCartRepository abandonedCartRepository,
            StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.abandonedCartRepository = abandonedCartRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "#days")
    public AdminAnalyticsDTO getDailyDashboardMetrics(int days) {
        log.info("[CACHE MISS] Computing dynamic dashboard metrics for admin console for last {} days...", days);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // 1. Calculate live conversion rate
        long totalCheckoutSessions = abandonedCartRepository.count();
        long recoveredCheckouts = abandonedCartRepository.countByIsRecoveredTrue();
        double conversionRate = totalCheckoutSessions > 0
                ? ((double) recoveredCheckouts / totalCheckoutSessions) * 100.0
                : 0.0;

        // 2. Fetch orders from current period and previous period to calculate sales growth
        LocalDateTime since = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime prevSince = LocalDate.now().minusDays((days * 2) - 1).atStartOfDay();
        
        List<Order> orders = orderRepository.findByOrderDateAfter(prevSince);

        // Group active orders in current period
        List<Order> currentPeriodOrders = orders.stream()
                .filter(o -> !o.getOrderDate().isBefore(since) && o.getOrderStatus() != OrderStatus.CANCELLED)
                .toList();

        List<Order> previousPeriodOrders = orders.stream()
                .filter(o -> o.getOrderDate().isBefore(since) && o.getOrderStatus() != OrderStatus.CANCELLED)
                .toList();

        BigDecimal currentRevenue = currentPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousRevenue = previousPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double salesGrowthPercent = 0.0;
        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowthPercent = ((currentRevenue.subtract(previousRevenue)).doubleValue() / previousRevenue.doubleValue()) * 100.0;
        } else if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowthPercent = 100.0; // 100% growth if there were no previous sales
        }

        // Group by LocalDate in memory and sum amounts
        Map<LocalDate, BigDecimal> revenueMap = currentPeriodOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().toLocalDate(),
                        Collectors.mapping(Order::getTotalAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        // Create sequential daily data points
        List<DailyRevenueDTO> revenueGraph = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            BigDecimal dailyRevenue = revenueMap.getOrDefault(date, BigDecimal.ZERO);
            revenueGraph.add(DailyRevenueDTO.builder()
                    .date(date.toString())
                    .revenue(dailyRevenue)
                    .build());
        }

        // 3. Fetch detailed low stock products
        List<LowStockProductDTO> lowStockProducts = productRepository.getLowStockProducts().stream()
                .map(p -> LowStockProductDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .stock(p.getStock())
                        .price(p.getPrice())
                        .build())
                .toList();

        // 4. Retrieve live active user count from Redis (default to 1)
        int activeUserCount = 1;
        try {
            java.util.Set<String> keys = redisTemplate.keys("active_user_session:*");
            activeUserCount = keys != null ? Math.max(keys.size(), 1) : 1;
        } catch (Exception e) {
            log.error("Failed to query live user sessions in Redis: {}", e.getMessage());
        }

        log.info("Dashboard stats computed. Growth: {}%, Low stock count: {}, Live users: {}", 
                salesGrowthPercent, lowStockProducts.size(), activeUserCount);

        return AdminAnalyticsDTO.builder()
                .todayRevenue(orderRepository.getTodayRevenue(startOfDay, endOfDay))
                .todayOrderCount(orderRepository.getTodayOrderCount(startOfDay, endOfDay))
                .pendingOrderCount(orderRepository.getPendingOrderCount())
                .lowStockProductCount((long) lowStockProducts.size())
                .conversionRate(conversionRate)
                .activeUserCount(activeUserCount)
                .salesGrowthPercent(salesGrowthPercent)
                .revenueGraph(revenueGraph)
                .lowStockProducts(lowStockProducts)
                .build();
    }
}