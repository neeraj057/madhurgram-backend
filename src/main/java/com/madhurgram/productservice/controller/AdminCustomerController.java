package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.dto.CustomerStatsDTO;
import com.madhurgram.productservice.service.AdminCustomerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@CrossOrigin(origins = "*")
public class AdminCustomerController {

    private final AdminCustomerService service;

    public AdminCustomerController(AdminCustomerService service) { this.service = service; }

    @GetMapping("/{phone}/history")
    public CustomerHistoryDTO getHistory(@PathVariable String phone) {
        return service.getCustomerHistory(phone);
    }
}