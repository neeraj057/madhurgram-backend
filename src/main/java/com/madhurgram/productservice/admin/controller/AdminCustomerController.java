package com.madhurgram.productservice.admin.controller;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;
import com.madhurgram.productservice.admin.service.AdminCustomerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.madhurgram.productservice.common.util.DataMaskingUtil;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/admin/customers")
@CrossOrigin(origins = "*")
public class AdminCustomerController {

    private final AdminCustomerService service;

    public AdminCustomerController(AdminCustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerStatsDTO> getAllCustomers(@RequestParam(required = false) String search) {
        List<CustomerStatsDTO> customers = service.getAllCustomerStats();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

        List<CustomerStatsDTO> processed = customers;
        if (!isSuperAdmin) {
            processed = customers.stream()
                .map(c -> new CustomerStatsDTO(
                    c.name(),
                    DataMaskingUtil.maskPhoneNumber(c.phoneNumber()),
                    c.totalOrders(),
                    c.totalSpent(),
                    c.lastOrderDate(),
                    c.vip(),
                    c.segment(),
                    c.favoriteProduct(),
                    c.favoriteProductQuantity()
                ))
                .toList();
        }

        if (search == null || search.isBlank()) {
            return processed.stream()
                .sorted(Comparator.comparing(CustomerStatsDTO::totalSpent).reversed())
                .toList();
        }

        String normalized = search.trim().toLowerCase();
        return processed.stream()
            .filter(c -> c.name().toLowerCase().contains(normalized)
                || c.phoneNumber().contains(search.trim()))
            .sorted(Comparator.comparing(CustomerStatsDTO::totalSpent).reversed())
            .toList();
    }

    @GetMapping("/{phone}/history")
    public CustomerHistoryDTO getHistory(@PathVariable String phone) {
        CustomerHistoryDTO history = service.getCustomerHistory(phone);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

        if (!isSuperAdmin) {
            return new CustomerHistoryDTO(
                history.name(),
                DataMaskingUtil.maskPhoneNumber(history.phoneNumber()),
                history.orderHistory(),
                history.totalSpent(),
                history.totalOrders()
            );
        }

        return history;
    }
}