package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.dto.OrderResponseDTO;
import com.madhurgram.productservice.entity.Order;
import com.madhurgram.productservice.entity.OrderItem;
import com.madhurgram.productservice.entity.OrderStatus;
import com.madhurgram.productservice.repository.OrderRepository;
import com.madhurgram.productservice.service.OrderService;
import com.madhurgram.productservice.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

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
// 🔒 2. Auto-Deduction Engine via Loose Coupling
        // जब आर्डर PENDING से CONFIRMED होगा, तभी स्टॉक डिडक्ट होगा भाई
        if (currentStatus == OrderStatus.PENDING && nextStatus == OrderStatus.CONFIRMED) {
            for (OrderItem item : order.getOrderItems()) {
                // 🚀 सीधे आपके मौजूदा ProductServiceImpl के एटॉमिक अपडेट मेथड को कॉल कर रहा है भाई
                productService.deductProductStock(item.getProductId(), item.getQuantity());
            }
        }

        // 🔄 3. Apply Status Change & Persist Order
        order.setOrderStatus(nextStatus);
        return orderRepository.save(order);
    }


    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        List<Order> orders = orderRepository.findByPhoneNumberOrderByOrderDateDesc(phoneNumber.trim());

        return orders.stream().map(order -> OrderResponseDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .pincode(order.getPincode())
                .cityState(order.getCityState())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .orderItems(order.getOrderItems().stream().map(item -> {
                    BigDecimal itemPrice = (item.getPrice() instanceof BigDecimal) 
                            ? (BigDecimal) (Object) item.getPrice() 
                            : BigDecimal.valueOf(Double.parseDouble(String.valueOf(item.getPrice())));

                    return OrderResponseDTO.ItemDTO.builder()
                            .id(item.getId())
                            .productName(item.getProductName())
                            .quantity(item.getQuantity())
                            .price(itemPrice)
                            .build();
                }).toList())
                .build()).toList();
    }
}