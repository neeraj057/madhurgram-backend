package com.madhurgram.productservice.order.service;

import java.util.List;
import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;

public interface OrderService {
    OrderResponseDTO placeOrder(Order order);

    List<OrderResponseDTO> getAllOrders();

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus status);

    List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber);

    OrderResponseDTO getOrderDetails(Long orderId);

    OrderResponseDTO updateOrderPaymentStatus(Long orderId, String paymentStatus, String transactionId);
}