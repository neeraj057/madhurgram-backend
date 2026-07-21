package com.madhurgram.productservice.order.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.madhurgram.productservice.common.dto.PageResponse;
import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.madhurgram.productservice.common.util.DataMaskingUtil;

import jakarta.validation.constraints.Pattern;

/**
 * Controller for placing, tracking, and updating orders.
 * 
 * <p>
 * Handles data masking of customer phone numbers for roles
 * other than SUPER_ADMIN.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order processing, tracking, and status update endpoints")
public class OrderController {

    private final OrderService orderService;

    /**
     * Constructor injection for OrderController.
     *
     * @param orderService order service for database operations
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Places a new customer order.
     * Rate limited to prevent spam.
     *
     * @param order the order payload details
     * @return the saved order object
     */
    @PostMapping("/place")
    @com.madhurgram.productservice.common.annotation.RateLimit(limit = 5, windowSeconds = 60)
    @Operation(summary = "Place a new order", description = "Submits a shopping order. Subject to rate limits.")
    public ResponseEntity<?> placeOrder(@jakarta.validation.Valid @RequestBody Order order) {
        log.info("Request: place order for customer '{}', phone: '{}'", DataMaskingUtil.maskName(order.getCustomerName()), DataMaskingUtil.maskPhoneNumber(order.getPhoneNumber()));
        try {
            OrderResponseDTO savedOrder = orderService.placeOrder(order);
            log.info("Order successfully placed with ID: {}", savedOrder.getId());
            return ResponseEntity.ok(savedOrder);
        } catch (IllegalArgumentException e) {
            log.warn("Order validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unhandled error placing order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get order statistics", description = "Retrieves global order statistics for the admin dashboard.")
    public ResponseEntity<com.madhurgram.productservice.order.dto.OrderStatsDTO> getOrderStats() {
        log.info("Request: get global order stats");
        com.madhurgram.productservice.order.dto.OrderStatsDTO stats = orderService.getOrderStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Fetches all orders, optionally paginated. Mask phone numbers if current user
     * is not SUPER_ADMIN.
     *
     * @param page optional page index (0-based)
     * @param size optional page size limit
     * @return list or page of orders
     */
    @GetMapping
    @Operation(summary = "List all orders", description = "Retrieves paginated orders. Phone numbers are masked for non-super-admins. Defaults to page=0, size=20 if not specified.")
    public ResponseEntity<PageResponse<OrderResponseDTO>> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        log.info("Request: list all orders (page={}, size={})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<OrderResponseDTO> paginated = orderService.getAllOrders(pageable);
        log.info("Returning paginated page {} with {} order(s)", page, paginated.getNumberOfElements());
        return ResponseEntity.ok(PageResponse.from(paginated));
    }

    /**
     * Updates status of an order.
     *
     * @param id     the ID of the order to update
     * @param status the new order status
     * @return the updated order
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Updates order status (e.g. PENDING, COMPLETED, CANCELLED) by ID.")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        log.info("Request: update order ID: {} to status: {}", id, status);
        try {
            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(id, status);
            log.info("Order ID: {} status updated successfully to: {}", id, status);
            return ResponseEntity.ok(updatedOrder);
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed for order status update (ID: {}): {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Order not found or error occurred while updating status (ID: {}): {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Fetches all orders for a specific customer by phone number.
     * Masks phone numbers for non-super-admins.
     *
     * @param phone the validated phone number
     * @return list of order responses
     */
    @GetMapping("/customer/{phone}")
    @Operation(summary = "Get orders by customer phone", description = "Retrieves orders for a customer using their phone number.")
    public ResponseEntity<List<OrderResponseDTO>> getCustomerOrders(
            @PathVariable @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.") String phone) {
        log.info("Request: fetch customer orders for phone='{}'", DataMaskingUtil.maskPhoneNumber(phone));

        List<OrderResponseDTO> customerOrders = orderService.getOrdersByCustomerPhone(phone.trim());
        log.info("Returning {} order(s) for customer", customerOrders.size());

        if (customerOrders.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(customerOrders);
    }

    /**
     * Fetches detailed information for a single order by ID.
     * Masks phone number for non-super-admins.
     *
     * @param id the order ID
     * @return the detailed order details DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order details by ID", description = "Fetches complete order details and items by ID. Phone masked for non-super-admins.")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long id) {
        log.info("Request: fetch order details for ID: {}", id);
        try {
            OrderResponseDTO dto = orderService.getOrderDetails(id);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            log.warn("Order details request failed for ID: {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}