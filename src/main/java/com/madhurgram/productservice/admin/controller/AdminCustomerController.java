package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;
import com.madhurgram.productservice.admin.service.AdminCustomerService;
import com.madhurgram.productservice.common.util.DataMaskingUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import java.util.Comparator;
import java.util.List;

/**
 * Admin controller for customer management operations.
 *
 * <p>Provides endpoints for listing customers and viewing individual customer history.
 * Phone numbers are masked for non-super-admin roles.
 *
 * <p><b>Production note:</b> Replace {@code @CrossOrigin} with a centralized
 * {@code CorsConfigurationSource} bean in your Security config before deploying.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/admin/customers")
@Tag(name = "Admin — Customers", description = "Customer analytics and history for admin panel")
public class AdminCustomerController {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/customers
    // -------------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "List all customers", description = "Returns customer stats sorted by total spend. Phone numbers are masked for non-super-admins.")
    public ResponseEntity<List<CustomerStatsDTO>> getAllCustomers(
            @RequestParam(required = false) String search) {

        log.info("Admin request: list customers, search='{}'", search);

        List<CustomerStatsDTO> customers = adminCustomerService.getAllCustomerStats();
        boolean isSuperAdmin = isSuperAdmin();

        List<CustomerStatsDTO> processed = isSuperAdmin
                ? customers
                : maskPhoneNumbers(customers);

        List<CustomerStatsDTO> result = filterAndSort(processed, search);

        log.info("Returning {} customer(s) [superAdmin={}]", result.size(), isSuperAdmin);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------------------------
    // GET /api/admin/customers/{phone}/history
    // -------------------------------------------------------------------------

    @GetMapping("/{phone}/history")
    @Operation(summary = "Get customer order history", description = "Returns full order history for a customer. Phone is masked for non-super-admins.")
    public ResponseEntity<CustomerHistoryDTO> getHistory(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone) {

        log.info("Admin request: customer history for phone='{}'", phone);

        CustomerHistoryDTO history = adminCustomerService.getCustomerHistory(phone);

        if (history == null) {
            log.warn("No history found for phone='{}'", phone);
            return ResponseEntity.notFound().build();
        }

        boolean isSuperAdmin = isSuperAdmin();
        CustomerHistoryDTO response = isSuperAdmin
                ? history
                : new CustomerHistoryDTO(
                        history.name(),
                        DataMaskingUtil.maskPhoneNumber(history.phoneNumber()),
                        history.orderHistory(),
                        history.totalSpent(),
                        history.totalOrders());

        log.info("Returning history for phone='{}' [superAdmin={}]", phone, isSuperAdmin);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the currently authenticated principal holds the
     * {@code SUPER_ADMIN} role (with or without the {@code ROLE_} prefix).
     */
    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_SUPER_ADMIN.equals(a.getAuthority())
                        || SUPER_ADMIN.equals(a.getAuthority()));
    }

    /** Masks phone numbers for all customers in the given list. */
    private List<CustomerStatsDTO> maskPhoneNumbers(List<CustomerStatsDTO> customers) {
        return customers.stream()
                .map(c -> new CustomerStatsDTO(
                        c.name(),
                        DataMaskingUtil.maskPhoneNumber(c.phoneNumber()),
                        c.totalOrders(),
                        c.totalSpent(),
                        c.lastOrderDate(),
                        c.vip(),
                        c.segment(),
                        c.favoriteProduct(),
                        c.favoriteProductQuantity()))
                .toList();
    }

    /** Filters by search term (name / phone) and sorts by total spend descending. */
    private List<CustomerStatsDTO> filterAndSort(List<CustomerStatsDTO> customers, String search) {
        if (search == null || search.isBlank()) {
            return customers.stream()
                    .sorted(Comparator.comparing(CustomerStatsDTO::totalSpent).reversed())
                    .toList();
        }

        String normalized = search.trim().toLowerCase();
        return customers.stream()
                .filter(c -> c.name().toLowerCase().contains(normalized)
                        || c.phoneNumber().contains(search.trim()))
                .sorted(Comparator.comparing(CustomerStatsDTO::totalSpent).reversed())
                .toList();
    }
}