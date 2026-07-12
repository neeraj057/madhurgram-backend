package com.madhurgram.productservice.returns.service.impl;

import com.madhurgram.productservice.audit.service.AuditLogService;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.entity.OrderItem;
import com.madhurgram.productservice.order.entity.OrderStatus;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.product.service.ProductService;
import com.madhurgram.productservice.returns.dto.ReturnRequestDTO;
import com.madhurgram.productservice.returns.entity.ReturnRequest;
import com.madhurgram.productservice.returns.mapper.ReturnMapper;
import com.madhurgram.productservice.returns.repository.ReturnRequestRepository;
import com.madhurgram.productservice.returns.service.ReturnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for administering product return requests, 
 * stock restocking, and automated digital UPI refund processes.
 */
@Slf4j
@Service
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRepository;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final AuditLogService auditLogService;
    private final ReturnMapper returnMapper;

    /**
     * Constructor injection for ReturnServiceImpl.
     *
     * @param returnRepository return request database repository
     * @param orderRepository  order database access
     * @param productService   product catalog stock modifier service
     * @param auditLogService  security audit logs manager
     * @param returnMapper     returns data mapper helper
     */
    public ReturnServiceImpl(
            ReturnRequestRepository returnRepository,
            OrderRepository orderRepository,
            ProductService productService,
            AuditLogService auditLogService,
            ReturnMapper returnMapper
    ) {
        this.returnRepository = returnRepository;
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.auditLogService = auditLogService;
        this.returnMapper = returnMapper;
    }

    /**
     * Submits a new return request for a given order ID and phone number.
     * Only delivered orders can be requested for return.
     *
     * @param orderId the order ID reference
     * @param phone   customer phone number verification
     * @param reason  return reasons description
     * @return the created return request DTO
     */
    @Override
    @Transactional
    public ReturnRequestDTO createReturnRequest(Long orderId, String phone, String reason) {
        log.info("Attempting to create return request for Order ID: {} and Phone: '{}'", orderId, phone);

        if (orderId == null || phone == null || phone.trim().isEmpty() || reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID, customer phone number, and return reason must not be blank.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        String normalizedOrderPhone = order.getPhoneNumber().trim().replaceAll("\\D+", "");
        String normalizedInputPhone = phone.trim().replaceAll("\\D+", "");

        if (normalizedOrderPhone.length() >= 10 && normalizedInputPhone.length() >= 10) {
            String suffixOrder = normalizedOrderPhone.substring(normalizedOrderPhone.length() - 10);
            String suffixInput = normalizedInputPhone.substring(normalizedInputPhone.length() - 10);
            if (!suffixOrder.equals(suffixInput)) {
                log.warn("Create return request rejected: phone suffix mismatch for Order ID: {}", orderId);
                throw new IllegalArgumentException("The phone number provided does not match the order records.");
            }
        } else if (!normalizedOrderPhone.equals(normalizedInputPhone)) {
            log.warn("Create return request rejected: phone number mismatch for Order ID: {}", orderId);
            throw new IllegalArgumentException("The phone number provided does not match the order records.");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            log.warn("Create return request rejected: order ID: {} status is not DELIVERED", orderId);
            throw new IllegalStateException("Only delivered orders can be requested for return. Current status: " + order.getOrderStatus());
        }

        Optional<ReturnRequest> existing = returnRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.warn("Create return request rejected: active return already exists for Order ID: {}", orderId);
            throw new IllegalStateException("A return request has already been filed for this Order ID.");
        }

        ReturnRequest request = ReturnRequest.builder()
                .orderId(orderId)
                .customerPhone(phone.trim())
                .reason(reason.trim())
                .status("PENDING")
                .build();

        ReturnRequest saved = returnRepository.save(request);
        log.info("Return request filed successfully with ID: {} for Order ID: {}", saved.getId(), orderId);
        auditLogService.log("CREATE_RETURN_REQUEST", String.valueOf(saved.getId()), "Filed return request for order ID " + orderId);
        return returnMapper.toDTO(saved);
    }

    /**
     * Resolves return request details for a specific order.
     *
     * @param orderId order ID identifier
     * @return return request if found, otherwise null
     */
    @Override
    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnRequestByOrderId(Long orderId) {
        log.info("Fetching return request details for order ID: {}", orderId);
        
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null.");
        }

        ReturnRequest request = returnRepository.findByOrderId(orderId).orElse(null);
        return returnMapper.toDTO(request);
    }

    /**
     * Lists all return requests sorted by creation date descending.
     *
     * @return list of return requests DTO
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getAllReturnRequests() {
        log.info("Admin request: list all return requests");
        List<ReturnRequest> list = returnRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return list.stream()
                .map(returnMapper::toDTO)
                .toList();
    }

    /**
     * Approves a customer return request, cancels order status, and generates mock UPI transaction code.
     * Restores stock automatically to the catalog.
     *
     * @param returnId target return identifier
     * @return approved return details DTO
     */
    @Override
    @Transactional
    public ReturnRequestDTO approveReturnRequest(Long returnId) {
        log.info("Approving return request ID: {}", returnId);

        if (returnId == null) {
            throw new IllegalArgumentException("Return Request ID cannot be null.");
        }

        ReturnRequest request = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found for ID: " + returnId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Return request is already processed. Current status: " + request.getStatus());
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Associated order not found for ID: " + request.getOrderId()));

        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());
        String refundTxnId = "REFUND-UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        request.setRefundTransactionId(refundTxnId);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus("REFUNDED");
        orderRepository.save(order);

        for (OrderItem item : order.getOrderItems()) {
            try {
                log.info("Restoring stock for Product ID: {} (Qty: {}) due to approved return.", item.getProductId(), item.getQuantity());
                productService.restoreProductStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore stock for Product ID: {} during return approval.", item.getProductId(), e);
            }
        }

        ReturnRequest saved = returnRepository.save(request);
        log.info("Return request ID: {} approved. Refund txn: {}", saved.getId(), refundTxnId);
        auditLogService.log("APPROVE_RETURN_REQUEST", String.valueOf(returnId), "Approved return & initiated refund " + refundTxnId);
        return returnMapper.toDTO(saved);
    }

    /**
     * Rejects a pending return request.
     *
     * @param returnId target return identifier
     * @return updated return details DTO
     */
    @Override
    @Transactional
    public ReturnRequestDTO rejectReturnRequest(Long returnId) {
        log.info("Rejecting return request ID: {}", returnId);

        if (returnId == null) {
            throw new IllegalArgumentException("Return Request ID cannot be null.");
        }

        ReturnRequest request = returnRepository.findById(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found for ID: " + returnId));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Return request is already processed. Current status: " + request.getStatus());
        }

        request.setStatus("REJECTED");
        request.setApprovedAt(LocalDateTime.now());

        ReturnRequest saved = returnRepository.save(request);
        auditLogService.log("REJECT_RETURN_REQUEST", String.valueOf(returnId), "Rejected return request");
        return returnMapper.toDTO(saved);
    }
}
