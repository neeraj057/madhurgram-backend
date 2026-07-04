package com.madhurgram.productservice.customer.dto;

import java.math.BigDecimal;
import java.util.List;

import com.madhurgram.productservice.order.dto.OrderSummaryDTO;

public record CustomerHistoryDTO(
    String name, 
    String phoneNumber, 
    List<OrderSummaryDTO> orderHistory, 
    BigDecimal totalSpent, 
    int totalOrders
) {}