package com.madhurgram.productservice.analytics.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAnalyticsDTO {
    private BigDecimal todayRevenue; // today revenue
    private Long todayOrderCount; // today order count
    private Long pendingOrderCount; // today pending order count
    private Long lowStockProductCount; // how much products are left (less than 5)
    private double conversionRate; // live order conversion rate (%)
    private int activeUserCount; // live user count
    private double salesGrowthPercent; // sales growth percent(%)
    private java.util.List<DailyRevenueDTO> revenueGraph; // past revenue (30 days)
    private java.util.List<LowStockProductDTO> lowStockProducts; // how much products are left (less than 5)
}