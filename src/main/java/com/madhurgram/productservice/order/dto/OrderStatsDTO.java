package com.madhurgram.productservice.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {
    private long totalOrders;
    private long todayOrders;
    private long yesterdayOrders;
    private long pendingOrders;
    private long processingOrders;
    private long completedOrders;
}
