package com.madhurgram.productservice.order.controller;

import java.util.List;
import com.madhurgram.productservice.order.dto.OrderResponseDTO;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = { "http://localhost:3000", "http://192.168.31.211:3000" })
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService; // 🧼 क्लीन एंड आर्किटेक्चरल

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {
        log.info("Received request to place order for customer: {} (Phone: {})", order.getCustomerName(), order.getPhoneNumber());
        try {
            Order savedOrder = orderService.placeOrder(order);
            log.info("Order placed successfully with ID: {}", savedOrder.getId());
            return ResponseEntity.ok(savedOrder);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid order placement request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to place order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<java.util.List<Order>> getAllOrders() {
        log.info("Received request to fetch all orders");
        List<Order> orders = orderService.getAllOrders();
        log.info("Successfully fetched {} orders", orders.size());
        return ResponseEntity.ok(orders);
    }

   @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        log.info("Received request to update order ID: {} to status: {}", id, status);
        try {
            Order updatedOrder = orderService.updateOrderStatus(id, status);
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

    // 🔍 GET: Fetch all orders for a specific customer by phone number
    @GetMapping("/customer/{phone}")
    public ResponseEntity<List<OrderResponseDTO>> getCustomerOrders(@PathVariable String phone) {
        log.info("Received request to fetch orders for phone number: {}", phone);
        if (phone == null || phone.trim().length() < 10) {
            log.warn("Invalid phone number format provided: {}", phone);
            throw new IllegalArgumentException("Invalid phone number format. Must be at least 10 digits.");
        }
        
        List<OrderResponseDTO> customerOrders = orderService.getOrdersByCustomerPhone(phone.trim());
        log.info("Fetched {} orders for customer phone: {}", customerOrders.size(), phone);
        
        if (customerOrders.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        
        return ResponseEntity.ok(customerOrders); // 200 OK
    }
}