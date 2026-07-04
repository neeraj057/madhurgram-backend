package com.madhurgram.productservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record CustomerHistoryDTO(
    String name, 
    String phoneNumber, 
    List<OrderSummaryDTO> orderHistory, 
    BigDecimal totalSpent, 
    int totalOrders
) {}