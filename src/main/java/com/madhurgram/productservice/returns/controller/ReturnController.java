package com.madhurgram.productservice.returns.controller;

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

    /**
     * Constructor injection for ReturnController.
     * Decoupled from direct repository access to follow strict MVC boundaries.
     *
     * @param returnService   returns management service
     */
    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    /**
     * Customer endpoint to request product return.
     *
     * @param orderId the order ID
     * @param phone   validated customer phone number
     * @param reason  reason for returning items
     * @return the created return request DTO details
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
    @PostMapping("/admin/{id}/reject")
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
        try {
            String svg = returnService.generateShippingLabelSvg(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/svg+xml"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"return_label_" + id + ".svg\"");
            
            log.info("Successfully exported Return Label ID: {} as SVG attachment", id);
            return new ResponseEntity<>(svg, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Label export failed for return ID: {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("<svg><text y=\"20\">" + e.getMessage() + "</text></svg>");
        }
    }
}
