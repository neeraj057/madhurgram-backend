package com.madhurgram.productservice.service;

import java.util.List;

import com.madhurgram.productservice.dto.OrderResponseDTO;
import com.madhurgram.productservice.entity.Order;

public interface OrderService {
    Order placeOrder(Order order);
    java.util.List<Order> getAllOrders();
    Order updateOrderStatus(Long orderId, com.madhurgram.productservice.entity.OrderStatus status);
    List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber);
}