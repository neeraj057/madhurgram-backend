package com.madhurgram.productservice.order.service;

import java.util.List;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;

public interface OrderService {
    Order placeOrder(Order order);

    java.util.List<Order> getAllOrders();

    Order updateOrderStatus(Long orderId, com.madhurgram.productservice.order.entity.OrderStatus status);

    List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber);

    OrderResponseDTO getOrderDetails(Long orderId);
}