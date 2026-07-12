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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for running business dashboard analytics queries,
 * sales growth estimations, conversions, and inventory stock tracking metrics.
 */
@Slf4j
@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AbandonedCartRepository abandonedCartRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor injection for AnalyticsServiceImpl.
     *
     * @param orderRepository         order repository dependency
     * @param productRepository       product repository dependency
     * @param abandonedCartRepository cart repository dependency
     * @param redisTemplate           redis cache template client
     */
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

    /**
     * Aggregates storefront dashboard metrics including sales, conversions, and
     * stock warnings.
     * Caches calculations to avoid heavy SQL computation.
     *
     * @param days metrics window standard duration (e.g. 7, 30 days)
     * @return calculated administration dashboard metrics DTO
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "analytics", key = "'dashboard_' + #days")
    public AdminAnalyticsDTO getDailyDashboardMetrics(int days) {
        log.info("[ANALYTICS] Starting dashboard computation for last {} days...", days);

        if (days <= 0) {
            throw new IllegalArgumentException("Analytics duration window must be a positive number of days.");
        }

        // 1. Calculate live conversion rate
        log.info("[ANALYTICS] Step 1: Querying abandoned cart repository counts...");
        long totalCheckoutSessions = abandonedCartRepository.count();
        long recoveredCheckouts = abandonedCartRepository.countByIsRecoveredTrue();
        double conversionRate = totalCheckoutSessions > 0
                ? ((double) recoveredCheckouts / totalCheckoutSessions) * 100.0
                : 0.0;
        log.info("[ANALYTICS] Step 1 complete. Conversion rate: {}", conversionRate);

        // 2. Fetch orders from current period and previous period to calculate sales
        // growth
        LocalDateTime since = LocalDate.now().minusDays(days - 1).atStartOfDay();
        LocalDateTime prevSince = LocalDate.now().minusDays((days * 2) - 1).atStartOfDay();

        log.info("[ANALYTICS] Step 2: Querying orders since: {}", prevSince);
        List<Order> orders = orderRepository.findByOrderDateAfter(prevSince);
        log.info("[ANALYTICS] Step 2: Orders queried: {}. Processing growth in memory...", orders.size());

        List<Order> currentPeriodOrders = orders.stream()
                .filter(o -> !o.getOrderDate().isBefore(since) && o.getOrderStatus() != OrderStatus.CANCELLED)
                .toList();

        List<Order> previousPeriodOrders = orders.stream()
                .filter(o -> o.getOrderDate().isBefore(since) && o.getOrderStatus() != OrderStatus.CANCELLED)
                .toList();

        // Period-based revenue (used for growth % comparison only)
        BigDecimal currentRevenue = currentPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousRevenue = previousPeriodOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double salesGrowthPercent = 0.0;
        if (previousRevenue.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowthPercent = ((currentRevenue.subtract(previousRevenue)).doubleValue()
                    / previousRevenue.doubleValue()) * 100.0;
        } else if (currentRevenue.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowthPercent = 100.0;
        }
        log.info("[ANALYTICS] Step 2 complete. Sales Growth: {}", salesGrowthPercent);

        // ✅ Actual TODAY's metrics (midnight → now), separate from growth window
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Order> todayOrders = currentPeriodOrders.stream()
                .filter(o -> !o.getOrderDate().isBefore(todayStart))
                .toList();

        BigDecimal todayRevenue = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long todayOrderCount = todayOrders.size();
        log.info("[ANALYTICS] Today's orders: {}, Today's revenue: {}", todayOrderCount, todayRevenue);

        // Group by LocalDate in memory and sum amounts
        Map<LocalDate, BigDecimal> revenueMap = currentPeriodOrders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getOrderDate().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)));

        List<DailyRevenueDTO> chartData = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            BigDecimal dayRevenue = revenueMap.getOrDefault(date, BigDecimal.ZERO);
            chartData.add(new DailyRevenueDTO(date.toString(), dayRevenue));
        }
        // reverse chart data to chronological order
        chartData = chartData.stream().sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

        // 3. Fetch low stock items count
        log.info("[ANALYTICS] Step 3: Fetching low stock products from database...");
        List<LowStockProductDTO> lowStockProducts = productRepository.getLowStockProducts().stream()
                .map(p -> new LowStockProductDTO(p.getId(), p.getName(), p.getStock(), p.getPrice()))
                .toList();
        log.info("[ANALYTICS] Step 3 complete. Low stock count: {}", lowStockProducts.size());

        // 4. Fetch active user session count from Redis
        log.info("[ANALYTICS] Step 4: Querying active user count from Redis Sorted Set...");
        long doubleTimeWindow = System.currentTimeMillis() - (15 * 60 * 1000);
        Long activeUsers = 0L;
        try {
            activeUsers = redisTemplate.opsForZSet().count("active_sessions", doubleTimeWindow, Double.MAX_VALUE);
            if (activeUsers == null) {
                activeUsers = 0L;
            }
        } catch (Exception e) {
            log.error("[ANALYTICS] Failed to query active users from Redis: {}", e.getMessage());
        }
        log.info("[ANALYTICS] Step 4 complete. Active users: {}", activeUsers);

        log.info("[ANALYTICS] Completing dashboard stats builder...");
        long pendingOrdersCount = currentPeriodOrders.stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING)
                .count();

        return AdminAnalyticsDTO.builder()
                .todayRevenue(todayRevenue)
                .todayOrderCount(todayOrderCount)
                .pendingOrderCount(pendingOrdersCount)
                .lowStockProductCount((long) lowStockProducts.size())
                .conversionRate(conversionRate)
                .activeUserCount(activeUsers.intValue())
                .salesGrowthPercent(salesGrowthPercent)
                .revenueGraph(chartData)
                .lowStockProducts(lowStockProducts)
                .build();
    }
}