package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.entity.Order;
import com.madhurgram.productservice.entity.OrderStatus;
import com.madhurgram.productservice.repository.OrderRepository;
import com.madhurgram.productservice.service.OrderService;
import com.madhurgram.productservice.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService; // 🔗 ProductService इंजेक्ट की भाई

    public OrderServiceImpl(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Override
    @Transactional // 🛡️ अगर एक भी आइटम का स्टॉक कम नहीं हुआ, तो पूरा आर्डर डेटाबेस से रोलबैक हो
                   // जाएगा!
    public Order placeOrder(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        // लूप चलाकर ProductService से स्टॉक कम करवा रहे हैं भाई
        for (var item : order.getOrderItems()) {
            try {
                productService.deductProductStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        "Product '" + item.getProductName() + "' is Out of Stock or insufficient inventory.");
            }

            // Parent-Child रिलेशनशिप सिंक
            item.setOrder(order);
        }

        return orderRepository.save(order);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<Order> getAllOrders() {
        return orderRepository.findAll(); // डेटाबेस से सारे ऑर्डर्स लाएगा
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus == OrderStatus.PENDING && nextStatus != OrderStatus.CONFIRMED
                && nextStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Pending order can only move to CONFIRMED or CANCELLED.");
        }
        if (currentStatus == OrderStatus.CONFIRMED && nextStatus != OrderStatus.SHIPPED
                && nextStatus != OrderStatus.PENDING && nextStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Confirmed order can only move to SHIPPED, PENDING, or CANCELLED.");
        }
        if (currentStatus == OrderStatus.SHIPPED && nextStatus != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Shipped order can only move to DELIVERED.");
        }
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot change status of an already DELIVERED or CANCELLED order.");
        }

        order.setOrderStatus(nextStatus);
        return orderRepository.save(order);
    }
}