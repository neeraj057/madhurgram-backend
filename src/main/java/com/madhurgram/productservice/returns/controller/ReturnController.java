package com.madhurgram.productservice.returns.controller;

import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.returns.dto.ReturnRequestDTO;
import com.madhurgram.productservice.returns.service.ReturnService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for submitting, approving, rejecting, and generating labels for product return requests.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/returns")
@Tag(name = "Product Returns", description = "Endpoints for initiating customer return requests and admin approvals")
public class ReturnController {

    private final ReturnService returnService;
    private final OrderRepository orderRepository;

    /**
     * Constructor injection for ReturnController.
     *
     * @param returnService   returns management service
     * @param orderRepository order database access
     */
    public ReturnController(ReturnService returnService, OrderRepository orderRepository) {
        this.returnService = returnService;
        this.orderRepository = orderRepository;
    }

    /**
     * Customer endpoint to request product return.
     *
     * @param orderId the order ID
     * @param phone   validated customer phone number
     * @param reason  reason for returning items
     * @return the created return request
     */
    @PostMapping
    @Operation(summary = "Submit return request", description = "Submits a new return request for a given order ID and customer phone verification.")
    public ResponseEntity<?> requestReturn(
            @RequestParam Long orderId,
            @RequestParam
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @RequestParam String reason
    ) {
        log.info("Request return: orderId={}, phone='{}', reason='{}'", orderId, phone, reason);
        try {
            ReturnRequestDTO request = returnService.createReturnRequest(orderId, phone, reason);
            log.info("Return request successfully submitted with ID: {}", request.getId());
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Failed to create return request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Retrieves the return request details for a specific order.
     *
     * @param orderId order reference ID
     * @return return request if found, otherwise 404
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get return request by order ID", description = "Fetches active return request details for an order ID.")
    public ResponseEntity<ReturnRequestDTO> getReturnRequest(@PathVariable Long orderId) {
        log.info("Request: fetch return request for order ID: {}", orderId);
        ReturnRequestDTO request = returnService.getReturnRequestByOrderId(orderId);
        if (request == null) {
            log.warn("No return request found for order ID: {}", orderId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    /**
     * Retrieves all return requests.
     *
     * @return list of return requests
     */
    @GetMapping("/admin/all")
    @Operation(summary = "List all return requests (Admin)", description = "Retrieves all customer return requests for the admin review panel.")
    public ResponseEntity<List<ReturnRequestDTO>> getAllReturnRequests() {
        log.info("Admin request: list all return requests");
        List<ReturnRequestDTO> list = returnService.getAllReturnRequests();
        log.info("Returning {} return request(s) to admin", list.size());
        return ResponseEntity.ok(list);
    }

    /**
     * Approves a customer return request.
     *
     * @param id return request ID
     * @return updated return request details
     */
    @PostMapping("/admin/{id}/approve")
    @Operation(summary = "Approve return request (Admin)", description = "Approves a pending return request by ID.")
    public ResponseEntity<?> approveReturn(@PathVariable Long id) {
        log.info("Admin request: approve return request ID: {}", id);
        try {
            ReturnRequestDTO approved = returnService.approveReturnRequest(id);
            log.info("Return request ID: {} successfully approved", id);
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            log.error("Failed to approve return request ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Rejects a customer return request.
     *
     * @param id return request ID
     * @return updated return request details
     */
    @PostMapping("/api/returns/admin/{id}/reject")
    @Operation(summary = "Reject return request (Admin)", description = "Rejects a pending return request by ID.")
    public ResponseEntity<?> rejectReturn(@PathVariable Long id) {
        log.info("Admin request: reject return request ID: {}", id);
        try {
            ReturnRequestDTO rejected = returnService.rejectReturnRequest(id);
            log.info("Return request ID: {} successfully rejected", id);
            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            log.error("Failed to reject return request ID: {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Generates a downloadable prepaid return shipping label as an SVG image.
     *
     * @param id return request ID
     * @return SVG string as attachment media response
     */
    @GetMapping(value = "/label/{id}", produces = "image/svg+xml")
    @Operation(summary = "Get prepaid return shipping label", description = "Generates and exports an SVG shipping label containing tracking barcodes and receiver details.")
    public ResponseEntity<String> getShippingLabel(@PathVariable Long id) {
        log.info("Request: export SVG return label for return request ID: {}", id);
        ReturnRequestDTO request = returnService.getAllReturnRequests().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (request == null) {
            log.warn("Label export failed: return request ID: {} not found", id);
            return ResponseEntity.notFound().build();
        }

        Order order = orderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Label export failed: order ID: {} not found", request.getOrderId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("<svg><text y=\"20\">Order not found</text></svg>");
        }

        String labelDate = request.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String customerName = order.getCustomerName();
        String addressLine1 = order.getAddress();
        String cityState = order.getCityState();
        String pincode = order.getPincode();
        String phone = order.getPhoneNumber();
        Long orderId = order.getId();

        // Beautiful SVG string with a mock barcode
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 400 550\" width=\"400\" height=\"550\">\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"530\" fill=\"#FFFFFF\" stroke=\"#000000\" stroke-width=\"3\" rx=\"10\"/>\n" +
                "  <rect x=\"10\" y=\"10\" width=\"380\" height=\"70\" fill=\"#111111\" rx=\"10\"/>\n" +
                "  <text x=\"200\" y=\"45\" font-family=\"sans-serif\" font-size=\"20\" font-weight=\"900\" fill=\"#D4AF37\" text-anchor=\"middle\" letter-spacing=\"1\">MADHURGRAM RETURNING</text>\n" +
                "  <text x=\"200\" y=\"63\" font-family=\"sans-serif\" font-size=\"9\" fill=\"#FAF9F5\" text-anchor=\"middle\">PREPAID RETURN SHIPPING LABEL</text>\n" +
                "  <text x=\"25\" y=\"110\" font-family=\"monospace\" font-size=\"14\" font-weight=\"bold\">CARRIER: DELHIVERY / SPEED POST</text>\n" +
                "  <text x=\"25\" y=\"130\" font-family=\"monospace\" font-size=\"12\">TRACKING: MG-RET-" + id + "-" + orderId + "</text>\n" +
                "  <text x=\"25\" y=\"150\" font-family=\"monospace\" font-size=\"12\">DATE: " + labelDate + "</text>\n" +
                "  <line x1=\"10\" y1=\"170\" x2=\"390\" y2=\"170\" stroke=\"#000000\" stroke-width=\"2\"/>\n" +
                "  <rect x=\"25\" y=\"185\" width=\"65\" height=\"18\" fill=\"#000000\" rx=\"3\"/>\n" +
                "  <text x=\"575\" y=\"198\" font-family=\"sans-serif\" font-size=\"10\" font-weight=\"bold\" fill=\"#FFFFFF\" text-anchor=\"middle\">SHIP TO</text>\n" +
                "  <!-- Correct position for text -->\n" +
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
                "  <text x=\"200\" y=\"477\" font-family=\"monospace\" font-size=\"12\" font-weight=\"bold\" text-anchor=\"middle\">*MG-RET-" + id + "-" + orderId + "*</text>\n" +
                "  <line x1=\"10\" y1=\"495\" x2=\"390\" y2=\"495\" stroke=\"#000000\" stroke-width=\"1\"/>\n" +
                "  <text x=\"200\" y=\"515\" font-family=\"sans-serif\" font-size=\"8\" fill=\"#666666\" font-style=\"italic\" text-anchor=\"middle\">Instructions: 1. Pack items securely. 2. Affix this label to the box. 3. Drop it off at your nearest speed post hub.</text>\n" +
                "</svg>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"return_label_" + id + ".svg\"");
        
        log.info("Successfully exported Return Label ID: {} as SVG attachment", id);
        return new ResponseEntity<>(svg, headers, HttpStatus.OK);
    }
}
