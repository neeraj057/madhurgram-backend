package com.madhurgram.productservice.order.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.dto.OrderStatsDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;

public interface OrderService {
    OrderResponseDTO placeOrder(Order order);

    List<OrderResponseDTO> getAllOrders();

    Page<OrderResponseDTO> getAllOrders(Pageable pageable);

    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus nextStatus);

    OrderStatsDTO getOrderStats();

    List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber);

    OrderResponseDTO getOrderDetails(Long orderId);

    OrderResponseDTO updateOrderPaymentStatus(Long orderId, String paymentStatus, String transactionId);
}