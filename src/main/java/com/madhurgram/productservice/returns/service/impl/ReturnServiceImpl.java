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
import java.time.format.DateTimeFormatter;
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

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_REFUNDED = "REFUNDED";
    
    private static final String AUDIT_CREATE_RETURN = "CREATE_RETURN_REQUEST";
    private static final String AUDIT_APPROVE_RETURN = "APPROVE_RETURN_REQUEST";
    private static final String AUDIT_REJECT_RETURN = "REJECT_RETURN_REQUEST";

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
                .status(STATUS_PENDING)
                .build();

        ReturnRequest saved = returnRepository.save(request);
        log.info("Return request filed successfully with ID: {} for Order ID: {}", saved.getId(), orderId);
        auditLogService.log(AUDIT_CREATE_RETURN, String.valueOf(saved.getId()), "Filed return request for order ID " + orderId);
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

        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Return request is already processed. Current status: " + request.getStatus());
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Associated order not found for ID: " + request.getOrderId()));

        request.setStatus(STATUS_APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        String refundTxnId = "REFUND-UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        request.setRefundTransactionId(refundTxnId);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(STATUS_REFUNDED);
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
        auditLogService.log(AUDIT_APPROVE_RETURN, String.valueOf(returnId), "Approved return & initiated refund " + refundTxnId);
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

        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Return request is already processed. Current status: " + request.getStatus());
        }

        request.setStatus(STATUS_REJECTED);
        request.setApprovedAt(LocalDateTime.now());

        ReturnRequest saved = returnRepository.save(request);
        auditLogService.log(AUDIT_REJECT_RETURN, String.valueOf(returnId), "Rejected return request");
        return returnMapper.toDTO(saved);
    }

    /**
     * Generates a downloadable prepaid return shipping label as an SVG image.
     *
     * @param returnRequestId target return request ID
     * @return SVG string
     */
    @Override
    @Transactional(readOnly = true)
    public String generateShippingLabelSvg(Long returnRequestId) {
        log.info("Generating SVG return label for return request ID: {}", returnRequestId);
        
        if (returnRequestId == null) {
            throw new IllegalArgumentException("Return Request ID must not be null.");
        }

        ReturnRequest request = returnRepository.findById(returnRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found with ID: " + returnRequestId));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Associated order not found for ID: " + request.getOrderId()));

        String labelDate = request.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String customerName = order.getCustomerName();
        String addressLine1 = order.getAddress();
        String cityState = order.getCityState();
        String pincode = order.getPincode();
        String phone = order.getPhoneNumber();
        Long orderId = order.getId();

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 550\" width=\"400\" height=\"550\">\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"530\" fill=\"#FFFFFF\" stroke=\"#000000\" stroke-width=\"3\" rx=\"10\"/>\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"70\" fill=\"#111111\" rx=\"10\"/>\n" +
                "  <text x=\"200\" y=\"45\" font-family=\"sans-serif\" font-size=\"20\" font-weight=\"900\" fill=\"#D4AF37\" text-anchor=\"middle\" letter-spacing=\"1\">MADHURGRAM RETURNING</text>\n" +
                "  <text x=\"200\" y=\"63\" font-family=\"sans-serif\" font-size=\"9\" fill=\"#FAF9F5\" text-anchor=\"middle\">PREPAID RETURN SHIPPING LABEL</text>\n" +
                "  <text x=\"25\" y=\"110\" font-family=\"monospace\" font-size=\"14\" font-weight=\"bold\">CARRIER: DELHIVERY / SPEED POST</text>\n" +
                "  <text x=\"25\" y=\"130\" font-family=\"monospace\" font-size=\"12\">TRACKING: MG-RET-" + returnRequestId + "-" + orderId + "</text>\n" +
                "  <text x=\"25\" y=\"150\" font-family=\"monospace\" font-size=\"12\">DATE: " + labelDate + "</text>\n" +
                "  <line x1=\"10\" y1=\"170\" x2=\"390\" y2=\"170\" stroke=\"#000000\" stroke-width=\"2\"/>\n" +
                "  <rect x=\"25\" y=\"185\" width=\"65\" height=\"18\" fill=\"#000000\" rx=\"3\"/>\n" +
                "  <text x=\"57\" y=\"198\" font-family=\"sans-serif\" font-size=\"9\" font-weight=\"bold\" fill=\"#FFFFFF\" text-anchor=\"middle\">SHIP TO</text>\n" +
                "  <text x=\"25\" y=\"225\" font-family=\"sans-serif\" font-size=\"12\" font-weight=\"bold\">MadhurGram Return Fulfillment Center</text>\n" +
                "  <text x=\"25\" y=\"240\" font-family=\"sans-serif\" font-size=\"11\">Plot 45, Sector B, Industrial Area</text>\n" +
                "  <text x=\"25\" y=\"255\" font-family=\"sans-serif\" font-size=\"11\">Indore, Madhya Pradesh - 452001</text>\n" +
                "  <text x=\"25\" y=\"270\" font-family=\"sans-serif\" font-size=\"11\">Contact: +91 98765 43210</text>\n" +
                "  <line x1=\"20\" y1=\"285\" x2=\"380\" y2=\"285\" stroke=\"#CCCCCC\" stroke-width=\"1\" stroke-dasharray=\"5,5\"/>\n" +
                "  <text x=\"25\" y=\"310\" font-family=\"sans-serif\" font-size=\"11\" font-weight=\"bold\" fill=\"#555555\">FROM (SENDER):</text>\n" +
                "  <text x=\"25\" y=\"325\" font-family=\"sans-serif\" font-size=\"12\" font-weight=\"bold\">" + customerName + "</text>\n" +
                "  <text x=\"25\" y=\"340\" font-family=\"sans-serif\" font-size=\"11\">" + addressLine1 + "</text>\n" +
                "  <text x=\"25\" y=\"355\" font-family=\"sans-serif\" font-size=\"11\">" + cityState + " - " + pincode + "</text>\n" +
                "  <text x=\"25\" y=\"370\" font-family=\"sans-serif\" font-size=\"11\">Phone: " + phone + "</text>\n" +
                "  <line x1=\"10\" y1=\"390\" x2=\"390\" y2=\"390\" stroke=\"#000000\" stroke-width=\"2\"/>\n" +
                "  <g transform=\"translate(55, 405)\">\n" +
                "    <rect x=\"0\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"5\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"8\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"15\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"20\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"25\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"33\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"38\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"43\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"48\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"55\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"60\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"65\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"75\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"80\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"85\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"92\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"98\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"104\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"112\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"117\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"122\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"128\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"135\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"140\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"145\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"155\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"160\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"165\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"172\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"178\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"184\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"192\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"197\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"202\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"208\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"215\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"220\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"225\" y=\"0\" width=\"6\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"235\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"240\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"245\" y=\"0\" width=\"4\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"252\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"258\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"264\" y=\"0\" width=\"5\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"272\" y=\"0\" width=\"2\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"277\" y=\"0\" width=\"1\" height=\"55\" fill=\"#000\"/>\n" +
                "    <rect x=\"282\" y=\"0\" width=\"3\" height=\"55\" fill=\"#000\"/>\n" +
                "  </g>\n" +
                "  <text x=\"200\" y=\"477\" font-family=\"monospace\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\">*MG-RET-" + returnRequestId + "-" + orderId + "*</text>\n" +
                "  <line x1=\"10\" y1=\"495\" x2=\"390\" y2=\"495\" stroke=\"#000000\" stroke-width=\"1\"/>\n" +
                "  <text x=\"200\" y=\"515\" font-family=\"sans-serif\" font-size=\"8\" fill=\"#666666\" font-style=\"italic\" text-anchor=\"middle\">Instructions: 1. Pack items securely. 2. Affix this label to the box. 3. Drop it off at your nearest speed post hub.</text>\n" +
                "</svg>";
    }
}
