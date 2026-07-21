package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;
import com.madhurgram.productservice.admin.service.AdminCustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.madhurgram.productservice.common.util.DataMaskingUtil;

import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * Admin controller for customer management operations.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/admin/customers")
@Tag(name = "Admin — Customers", description = "Customer analytics and history for admin panel")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    @GetMapping
    @Operation(summary = "List all customers", description = "Returns customer stats sorted by total spend. Phone numbers are masked for non-super-admins.")
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        log.info("Admin request: list customers, search='{}', page={}, size={}", search, page, size);

        if (page != null && size != null) {
            Page<CustomerStatsDTO> paginated = adminCustomerService.getCustomers(search, page, size);
            return ResponseEntity.ok(paginated);
        } else {
            List<CustomerStatsDTO> customers = adminCustomerService.getCustomers(search);
            return ResponseEntity.ok(customers);
        }
    }

    @GetMapping("/{phone}/history")
    @Operation(summary = "Get customer order history", description = "Returns full order history for a customer. Phone is masked for non-super-admins.")
    public ResponseEntity<CustomerHistoryDTO> getHistory(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format.")
            String phone) {

        log.info("Admin request: customer history for phone='{}'", DataMaskingUtil.maskPhoneNumber(phone));

        CustomerHistoryDTO history = adminCustomerService.getCustomerHistory(phone);

        if (history == null) {
            log.warn("No history found for phone='{}'", DataMaskingUtil.maskPhoneNumber(phone));
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(history);
    }
}