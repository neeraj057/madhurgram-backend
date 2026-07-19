package com.madhurgram.productservice.admin.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;

public interface AdminCustomerService {
    // Admin for list of all customers
    List<CustomerStatsDTO> getAllCustomerStats();

    Page<CustomerStatsDTO> getAllCustomerStats(Pageable pageable);

    // customer history by phone number
    CustomerHistoryDTO getCustomerHistory(String phoneNumber);

}
