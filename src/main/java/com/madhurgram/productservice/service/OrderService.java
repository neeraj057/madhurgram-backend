package com.madhurgram.productservice.service;

import com.madhurgram.productservice.entity.Order;

public interface OrderService {
    Order placeOrder(Order order);
    java.util.List<Order> getAllOrders();
    Order updateOrderStatus(Long orderId, com.madhurgram.productservice.entity.OrderStatus status);
}