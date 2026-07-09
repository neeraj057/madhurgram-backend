package com.madhurgram.productservice.order.service.impl;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderService;
import com.madhurgram.productservice.order.service.OrderNotificationService;
import com.madhurgram.productservice.product.service.ProductService;
import org.springframework.context.ApplicationEventPublisher;
import com.madhurgram.productservice.order.event.OrderConfirmedEvent;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final com.madhurgram.productservice.cart.service.AbandonedCartService abandonedCartService;
    private final OrderNotificationService orderNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.madhurgram.productservice.audit.service.AuditLogService auditLogService;

    public OrderServiceImpl(
            OrderRepository orderRepository, 
            ProductService productService, 
            com.madhurgram.productservice.cart.service.AbandonedCartService abandonedCartService,
            OrderNotificationService orderNotificationService,
            ApplicationEventPublisher eventPublisher,
            com.madhurgram.productservice.audit.service.AuditLogService auditLogService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.abandonedCartService = abandonedCartService;
        this.orderNotificationService = orderNotificationService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional // 🛡️ अगर एक भी आइटम का स्टॉक कम नहीं हुआ, तो पूरा आर्डर डेटाबेस से रोलबैक हो जाएगा!
    @CacheEvict(value = "analytics", allEntries = true)
    public Order placeOrder(Order order) {
        log.info("Processing order placement for customer: {}", order.getCustomerName());
        
        // 🛡️ Resolve Lombok builder-default gotcha: initialize null fields
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus("PENDING");
        }
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING);
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            log.warn("Order placement failed: Cart is empty.");
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        // लूप चलाकर ProductService से स्टॉक कम करवा रहे हैं भाई
        for (var item : order.getOrderItems()) {
            try {
                log.info("Deducting stock for Product ID: {}, Quantity: {}", item.getProductId(), item.getQuantity());
                productService.deductProductStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                log.warn("Stock deduction failed for Product ID: {}, Name: {}. Error: {}", item.getProductId(),
                        item.getProductName(), e.getMessage());
                throw new RuntimeException(
                        "Product '" + item.getProductName() + "' is Out of Stock or insufficient inventory.");
            }

            // Parent-Child रिलेशनशिप सिंक
            item.setOrder(order);
        }

        Order saved = orderRepository.save(order);
        log.info("Order saved in database with ID: {}", saved.getId());

        try {
            log.info("Triggering cart recovery finalization for phone: {}", order.getPhoneNumber());
            abandonedCartService.markAsRecovered(order.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to finalize cart recovery for phone {}: {}", order.getPhoneNumber(), e.getMessage());
        }

        return saved;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<Order> getAllOrders() {
        log.info("Fetching all orders with their items in a single query.");
        return orderRepository.findAllWithItems(); // Single query join fetch optimization
    }

    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public Order updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        log.info("Updating status for order ID: {} to {}", orderId, nextStatus);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        log.info("Order ID: {} current status is: {}", orderId, currentStatus);

        if (!currentStatus.isValidTransition(nextStatus)) {
            log.warn("Invalid transition: Cannot move order ID: {} from {} to {}", orderId, currentStatus, nextStatus);
            throw new IllegalArgumentException(
                    "Cannot change status of order from " + currentStatus + " to " + nextStatus);
        }

        // 🔄 2. Restore Stock if order is Cancelled
        if (nextStatus == OrderStatus.CANCELLED
                && (currentStatus == OrderStatus.PENDING 
                    || currentStatus == OrderStatus.CONFIRMED 
                    || currentStatus == OrderStatus.SHIPPED 
                    || currentStatus == OrderStatus.OUT_FOR_DELIVERY)) {
            log.info("Order ID: {} is being cancelled. Restoring stock for all items...", orderId);
            for (OrderItem item : order.getOrderItems()) {
                log.info("Restoring stock for product ID: {} (Quantity: {})", item.getProductId(), item.getQuantity());
                productService.restoreProductStock(item.getProductId(), item.getQuantity());
            }
        }

        // 🔄 3. Apply Status Change & Persist Order
        order.setOrderStatus(nextStatus);
        Order saved = orderRepository.save(order);
        log.info("Order ID: {} status updated successfully from {} to {}", orderId, currentStatus, nextStatus);
        
        auditLogService.log("UPDATE_ORDER_STATUS", String.valueOf(orderId), 
                "Status transitioned from " + currentStatus + " to " + nextStatus);
        
        // 🔄 4. Trigger Notification Bridge
        try {
            orderNotificationService.sendOrderStatusNotification(saved, nextStatus);
        } catch (Exception e) {
            log.error("Failed to trigger order notification for order ID: {}. Error: {}", orderId, e.getMessage());
        }

        // 🔄 5. Publish Event to trigger decoupled components (such as logistics pickup)
        if (nextStatus == OrderStatus.CONFIRMED) {
            log.info("Publishing OrderConfirmedEvent for Order ID: {}", orderId);
            eventPublisher.publishEvent(new OrderConfirmedEvent(this, saved));
        }
        
        return saved;
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
                .trackingNumber(order.getTrackingNumber())
                .courierName(order.getCourierName())
                .paymentStatus(order.getPaymentStatus())
                .paymentTransactionId(order.getPaymentTransactionId())
                .orderItems(order.getOrderItems().stream().map(item -> OrderResponseDTO.ItemDTO.builder()
                        .id(item.getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build()).toList())
                .build()).toList();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        return OrderResponseDTO.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .phoneNumber(order.getPhoneNumber())
                .address(order.getAddress())
                .pincode(order.getPincode())
                .cityState(order.getCityState())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .trackingNumber(order.getTrackingNumber())
                .courierName(order.getCourierName())
                .paymentStatus(order.getPaymentStatus())
                .paymentTransactionId(order.getPaymentTransactionId())
                .orderItems(order.getOrderItems().stream().map(item -> OrderResponseDTO.ItemDTO.builder()
                        .id(item.getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build()).toList())
                .build();
    }

}