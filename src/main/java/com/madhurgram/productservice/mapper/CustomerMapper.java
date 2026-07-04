package com.madhurgram.productservice.mapper;

import com.madhurgram.productservice.dto.*;
import com.madhurgram.productservice.entity.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomerMapper {

    public CustomerHistoryDTO toHistoryDTO(String phone, List<Order> orders) {
        List<OrderSummaryDTO> history = orders.stream()
            .map(o -> new OrderSummaryDTO(o.getId(), o.getOrderDate(), o.getTotalAmount(), o.getOrderStatus()))
            .toList();

        BigDecimal total = history.stream().map(OrderSummaryDTO::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerHistoryDTO(orders.get(0).getCustomerName(), phone, history, total, history.size());
    }

    public CustomerStatsDTO toStatsDTO(String phone, List<Order> orders) {
        return new CustomerStatsDTO(
            orders.get(0).getCustomerName(),
            phone,
            orders.size(),
            orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
            orders.stream().map(Order::getOrderDate).max(LocalDateTime::compareTo).orElse(null)
        );
    }
}