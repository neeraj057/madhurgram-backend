package com.madhurgram.productservice.mapper;

import com.madhurgram.productservice.dto.*;
import com.madhurgram.productservice.entity.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        BigDecimal totalSpent = orders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime lastOrderDate = orders.stream().map(Order::getOrderDate).max(LocalDateTime::compareTo).orElse(null);
        boolean vip = orders.size() >= 8 || totalSpent.compareTo(BigDecimal.valueOf(15000)) >= 0;
        String segment = vip ? "VIP" : totalSpent.compareTo(BigDecimal.valueOf(8000)) >= 0 || orders.size() >= 5 ? "LOYAL" : "REGULAR";

        Map<String, Integer> favorite = orders.stream()
            .flatMap(order -> order.getOrderItems().stream())
            .collect(Collectors.groupingBy(item -> item.getProductName(), Collectors.summingInt(item -> item.getQuantity())));

        String favoriteProduct = favorite.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse("No products yet");

        int favoriteProductQuantity = favorite.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getValue)
            .orElse(0);

        return new CustomerStatsDTO(
            orders.get(0).getCustomerName(),
            phone,
            orders.size(),
            totalSpent,
            lastOrderDate,
            vip,
            segment,
            favoriteProduct,
            favoriteProductQuantity
        );
    }
}