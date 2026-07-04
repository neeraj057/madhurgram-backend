package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.dto.CustomerStatsDTO;
import com.madhurgram.productservice.service.AdminCustomerService;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{phone}/history")
    public CustomerHistoryDTO getHistory(@PathVariable String phone) {
        return service.getCustomerHistory(phone);
    }
}