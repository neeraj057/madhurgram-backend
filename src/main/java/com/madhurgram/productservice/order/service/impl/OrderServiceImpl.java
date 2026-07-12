package com.madhurgram.productservice.order.service.impl;

import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.mapper.OrderMapper;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.order.service.OrderService;
import com.madhurgram.productservice.order.service.OrderNotificationService;
import com.madhurgram.productservice.product.service.ProductService;
import org.springframework.context.ApplicationEventPublisher;
import com.madhurgram.productservice.order.event.OrderConfirmedEvent;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.madhurgram.productservice.product.repository.ProductRepository;
import com.madhurgram.productservice.product.entity.Product;
import com.madhurgram.productservice.logistics.service.LogisticsService;

/**
 * Service implementation for managing customer shopping orders, calculations, 
 * stock deductions, logistics integrations, and status update flows.
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String PAYMENT_COD = "COD";
    
    private static final String DEFAULT_HSN_CODE = "0000";
    private static final BigDecimal DEFAULT_GST_RATE = BigDecimal.valueOf(5.00);

    private static final String AUDIT_ACTION_PLACE_COD = "PLACE_ORDER_COD";
    private static final String AUDIT_ACTION_UPDATE_STATUS = "UPDATE_ORDER_STATUS";

    // Validates if delivery state is Uttar Pradesh (UP) using word boundaries to avoid false positives (e.g. Tirupati containing 'up')
    private static final Pattern UP_STATE_PATTERN = Pattern.compile("\\b(uttar\\s+pradesh|u\\.p\\.|up)\\b", Pattern.CASE_INSENSITIVE);

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final ProductRepository productRepository;
    private final LogisticsService logisticsService;
    private final com.madhurgram.productservice.cart.service.AbandonedCartService abandonedCartService;
    private final OrderNotificationService orderNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final com.madhurgram.productservice.audit.service.AuditLogService auditLogService;
    private final com.madhurgram.productservice.marketing.repository.ReviewRequestRepository reviewRequestRepository;
    private final com.madhurgram.productservice.coupon.service.CouponService couponService;
    private final OrderMapper orderMapper;

    /**
     * Constructor injection for OrderServiceImpl.
     *
     * @param orderRepository         order database repository
     * @param productService          product catalog service
     * @param productRepository       product repository
     * @param logisticsService        logistics operations service
     * @param abandonedCartService    abandoned cart tracking service
     * @param orderNotificationService order notifications dispatch service
     * @param eventPublisher          application event dispatcher
     * @param auditLogService         security audit logger service
     * @param reviewRequestRepository review requests scheduling repository
     * @param couponService           discount coupons service
     * @param orderMapper             order mapper utility
     */
    public OrderServiceImpl(
            OrderRepository orderRepository, 
            ProductService productService, 
            ProductRepository productRepository,
            LogisticsService logisticsService,
            com.madhurgram.productservice.cart.service.AbandonedCartService abandonedCartService,
            OrderNotificationService orderNotificationService,
            ApplicationEventPublisher eventPublisher,
            com.madhurgram.productservice.audit.service.AuditLogService auditLogService,
            @org.springframework.context.annotation.Lazy com.madhurgram.productservice.marketing.repository.ReviewRequestRepository reviewRequestRepository,
            com.madhurgram.productservice.coupon.service.CouponService couponService,
            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.productRepository = productRepository;
        this.logisticsService = logisticsService;
        this.abandonedCartService = abandonedCartService;
        this.orderNotificationService = orderNotificationService;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
        this.reviewRequestRepository = reviewRequestRepository;
        this.couponService = couponService;
        this.orderMapper = orderMapper;
    }

    /**
     * Submits and validates a new customer checkout order.
     * Deducts stock and triggers automatic low inventory purchase orders if limits crossed.
     *
     * @param order raw checkout order details
     * @return order details response DTO
     */
    @Override
    @Transactional
    public OrderResponseDTO placeOrder(Order order) {
        log.info("Processing order placement for customer: {}", order.getCustomerName());
        
        if (order.getPaymentStatus() == null) {
            order.setPaymentStatus(STATUS_PENDING);
        }
        
        if (PAYMENT_COD.equalsIgnoreCase(order.getPaymentStatus())) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
        } else if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING);
        }

        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            log.warn("Order placement failed: Cart is empty.");
            throw new IllegalArgumentException("Cart cannot be empty.");
        }

        if (order.getLatitude() != null && order.getLongitude() != null) {
            double lat = order.getLatitude();
            double lng = order.getLongitude();
            log.info("Validating delivery address coordinates: Lat: {}, Lng: {}", lat, lng);
            if (lat < 6.0 || lat > 38.0 || lng < 68.0 || lng > 98.0) {
                log.warn("Order placement rejected: Coordinates ({}, {}) are outside India's boundaries.", lat, lng);
                throw new IllegalArgumentException("Delivery address coordinates are outside India's serviceable region.");
            }
        }

        // Secure state regex match check to prevent false positives (e.g., 'Tirupati, AP' matching 'up')
        boolean isIntraState = false;
        if (order.getCityState() != null) {
            if (UP_STATE_PATTERN.matcher(order.getCityState()).find()) {
                isIntraState = true;
            }
        }
        log.info("Place of Supply resolved. Is Intra-State (UP): {}", isIntraState);

        BigDecimal totalItemsAmount = BigDecimal.ZERO;
        for (var item : order.getOrderItems()) {
            totalItemsAmount = totalItemsAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountVal = BigDecimal.ZERO;
        if (order.getCouponCode() != null && !order.getCouponCode().trim().isEmpty()) {
            com.madhurgram.productservice.coupon.dto.CouponDTO coupon = couponService.validateCoupon(
                    order.getCouponCode(), order.getPhoneNumber(), totalItemsAmount);
            BigDecimal discountPercent = coupon.getDiscountPercentage();
            discountVal = totalItemsAmount.multiply(discountPercent).divide(BigDecimal.valueOf(100.0), 2, java.math.RoundingMode.HALF_UP);
            order.setCouponCode(coupon.getCode());
            order.setDiscountAmount(discountVal);
        } else {
            order.setCouponCode(null);
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        BigDecimal finalTotal = totalItemsAmount.subtract(discountVal);
        order.setTotalAmount(finalTotal);

        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;

        for (var item : order.getOrderItems()) {
            try {
                log.info("Deducting stock for Product ID: {}, Quantity: {}", item.getProductId(), item.getQuantity());
                productService.deductProductStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                log.warn("Stock deduction failed for Product ID: {}, Name: {}. Error: {}", item.getProductId(),
                        item.getProductName(), e.getMessage());
                throw new IllegalArgumentException(
                        "Product '" + item.getProductName() + "' is Out of Stock or has insufficient inventory.");
            }

            item.setOrder(order);

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            String hsnCode = DEFAULT_HSN_CODE;
            BigDecimal gstRate = DEFAULT_GST_RATE;

            if (product != null && product.getHsnTaxMaster() != null) {
                hsnCode = product.getHsnTaxMaster().getHsnCode();
                gstRate = product.getHsnTaxMaster().getGstRate();
            }

            item.setHsnCode(hsnCode);
            item.setGstRate(gstRate);

            BigDecimal totalItemPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            BigDecimal divisor = BigDecimal.ONE.add(gstRate.divide(BigDecimal.valueOf(100.0), 4, java.math.RoundingMode.HALF_UP));
            BigDecimal taxableValue = totalItemPrice.divide(divisor, 2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalTax = totalItemPrice.subtract(taxableValue);

            item.setTaxableAmount(taxableValue);
            taxableTotal = taxableTotal.add(taxableValue);

            if (isIntraState) {
                BigDecimal halfTax = totalTax.divide(BigDecimal.valueOf(2.0), 2, java.math.RoundingMode.HALF_UP);
                item.setCgstAmount(halfTax);
                item.setSgstAmount(halfTax);
                item.setIgstAmount(BigDecimal.ZERO);

                cgstTotal = cgstTotal.add(halfTax);
                sgstTotal = sgstTotal.add(halfTax);
            } else {
                item.setCgstAmount(BigDecimal.ZERO);
                item.setSgstAmount(BigDecimal.ZERO);
                item.setIgstAmount(totalTax);

                igstTotal = igstTotal.add(totalTax);
            }
        }

        if (discountVal.compareTo(BigDecimal.ZERO) > 0 && totalItemsAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxRatio = finalTotal.divide(totalItemsAmount, 6, java.math.RoundingMode.HALF_UP);
            taxableTotal = taxableTotal.multiply(taxRatio).setScale(2, java.math.RoundingMode.HALF_UP);
            cgstTotal = cgstTotal.multiply(taxRatio).setScale(2, java.math.RoundingMode.HALF_UP);
            sgstTotal = sgstTotal.multiply(taxRatio).setScale(2, java.math.RoundingMode.HALF_UP);
            igstTotal = igstTotal.multiply(taxRatio).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        order.setTaxableAmount(taxableTotal);
        order.setCgstTotal(cgstTotal);
        order.setSgstTotal(sgstTotal);
        order.setIgstTotal(igstTotal);

        Order saved = orderRepository.save(order);
        log.info("Order saved in database with ID: {}", saved.getId());

        if (saved.getCouponCode() != null) {
            try {
                couponService.recordCouponUsage(saved.getCouponCode(), saved.getPhoneNumber(), saved.getId());
            } catch (Exception e) {
                log.error("Failed to record coupon usage for order ID {}: {}", saved.getId(), e.getMessage());
            }
        }

        if (OrderStatus.CONFIRMED.equals(saved.getOrderStatus())) {
            auditLogService.log(AUDIT_ACTION_PLACE_COD, String.valueOf(saved.getId()), "Order placed via Cash on Delivery");

            try {
                orderNotificationService.sendOrderStatusNotification(saved, OrderStatus.CONFIRMED);
            } catch (Exception e) {
                log.error("Failed to send COD order confirmation notification for order ID: {}. Error: {}", saved.getId(), e.getMessage());
            }

            log.info("Publishing OrderConfirmedEvent for COD Order ID: {}", saved.getId());
            eventPublisher.publishEvent(new com.madhurgram.productservice.order.event.OrderConfirmedEvent(this, saved));
        }

        try {
            log.info("Triggering cart recovery finalization for phone: {}", order.getPhoneNumber());
            abandonedCartService.markAsRecovered(order.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to finalize cart recovery for phone {}: {}", order.getPhoneNumber(), e.getMessage());
        }

        return orderMapper.toResponseDTO(saved);
    }

    /**
     * Fetches all orders with their items in a single join query.
     *
     * @return a list of all order summaries DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        log.info("Fetching all orders with their items in a single optimized query.");
        List<Order> orders = orderRepository.findAllWithItems();
        return orders.stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    /**
     * Modifies the current execution status of an active customer order.
     * Restores stock automatically on order cancellation.
     *
     * @param orderId    target order identifier
     * @param nextStatus new status value to apply
     * @return updated order details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatus nextStatus) {
        log.info("Updating status for order ID: {} to {}", orderId, nextStatus);
        
        if (orderId == null || nextStatus == null) {
            log.warn("Update status aborted: Order ID or target status parameter is null");
            throw new IllegalArgumentException("Order ID and target status must not be null.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        log.info("Order ID: {} current status is: {}", orderId, currentStatus);

        if (!currentStatus.isValidTransition(nextStatus)) {
            log.warn("Invalid transition: Cannot move order ID: {} from {} to {}", orderId, currentStatus, nextStatus);
            throw new IllegalArgumentException(
                    "Cannot change status of order from " + currentStatus + " to " + nextStatus);
        }

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

        if (nextStatus == OrderStatus.SHIPPED) {
            log.info("Order ID: {} status updating to SHIPPED. Initiating logistics scheduling...", orderId);
            try {
                logisticsService.scheduleOrderPickup(order);
            } catch (Exception e) {
                log.error("Failed to execute logistics booking for Order ID: {}. Error: {}", orderId, e.getMessage(), e);
            }
        }

        order.setOrderStatus(nextStatus);
        Order saved = orderRepository.save(order);
        log.info("Order ID: {} status updated successfully from {} to {}", orderId, currentStatus, nextStatus);
        
        auditLogService.log(AUDIT_ACTION_UPDATE_STATUS, String.valueOf(orderId), 
                "Status transitioned from " + currentStatus + " to " + nextStatus);
        
        try {
            orderNotificationService.sendOrderStatusNotification(saved, nextStatus);
        } catch (Exception e) {
            log.error("Failed to trigger order notification for order ID: {}. Error: {}", orderId, e.getMessage());
        }

        if (nextStatus == OrderStatus.DELIVERED) {
            log.info("Order ID: {} has been DELIVERED. Scheduling Google Review request...", orderId);
            try {
                if (reviewRequestRepository.findByOrderId(orderId).isEmpty()) {
                    com.madhurgram.productservice.marketing.entity.ReviewRequest reviewRequest = com.madhurgram.productservice.marketing.entity.ReviewRequest.builder()
                            .orderId(orderId)
                            .customerName(saved.getCustomerName())
                            .customerPhone(saved.getPhoneNumber())
                            .status(STATUS_PENDING)
                            .scheduledAt(LocalDateTime.now().plusDays(1))
                            .build();
                    reviewRequestRepository.save(reviewRequest);
                    log.info("Successfully scheduled Google Review request for Order ID: {}", orderId);
                }
            } catch (Exception e) {
                log.error("Failed to schedule Google Review request for Order ID: {}", orderId, e);
            }
        }

        if (nextStatus == OrderStatus.CONFIRMED) {
            log.info("Publishing OrderConfirmedEvent for Order ID: {}", orderId);
            eventPublisher.publishEvent(new OrderConfirmedEvent(this, saved));
        }
        
        if (saved.getOrderItems() != null) {
            saved.getOrderItems().size();
        }
        
        return orderMapper.toResponseDTO(saved);
    }

    /**
     * Resolves orders associated with a customer phone.
     *
     * @param phoneNumber customer phone number
     * @return a list of order responses DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByCustomerPhone(String phoneNumber) {
        log.info("Fetching orders by customer phone number: '{}'", phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Lookup aborted: phone number parameter is blank");
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        List<Order> orders = orderRepository.findByPhoneNumberOrderByOrderDateDesc(phoneNumber.trim());
        return orders.stream()
                .map(orderMapper::toResponseDTO)
                .toList();
    }

    /**
     * Resolves detailed summary payload for a single order by ID.
     *
     * @param orderId target order identifier
     * @return detailed order details DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetails(Long orderId) {
        log.info("Fetching order details for ID: {}", orderId);
        
        if (orderId == null) {
            log.warn("Details lookup aborted: orderId parameter is null");
            throw new IllegalArgumentException("Order ID cannot be null.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
        return orderMapper.toResponseDTO(order);
    }

    /**
     * Updates order payment details and resolves order status updates based on payment result.
     *
     * @param orderId       target order identifier
     * @param paymentStatus new payment status to record
     * @param transactionId gateway transaction identifier
     * @return updated order details DTO
     */
    @Override
    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public OrderResponseDTO updateOrderPaymentStatus(Long orderId, String paymentStatus, String transactionId) {
        log.info("Updating payment status for order ID: {} to {} (Transaction: {})", orderId, paymentStatus, transactionId);

        if (orderId == null || paymentStatus == null) {
            log.warn("Payment status update aborted: Order ID or payment status parameter is null");
            throw new IllegalArgumentException("Order ID and payment status parameters must not be null.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        order.setPaymentStatus(paymentStatus);
        if (transactionId != null) {
            order.setPaymentTransactionId(transactionId);
        }
        orderRepository.save(order);

        if ("COMPLETED".equalsIgnoreCase(paymentStatus)) {
            // Enforce lifecycle update: Transition PENDING to CONFIRMED
            updateOrderStatus(orderId, OrderStatus.CONFIRMED);
            log.info("Order ID: {} successfully CONFIRMED via successful payment webhook processing", orderId);
        } else if ("FAILED".equalsIgnoreCase(paymentStatus)) {
            // Transition order to CANCELLED (which automatically releases stock)
            updateOrderStatus(orderId, OrderStatus.CANCELLED);
            log.info("Order ID: {} successfully CANCELLED due to failed payment webhook processing", orderId);
        }

        return orderMapper.toResponseDTO(order);
    }
}