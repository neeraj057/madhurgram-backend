package com.madhurgram.productservice.admin.service;

import java.util.List;
import org.springframework.data.domain.Page;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;

public interface AdminCustomerService {
    /**
     * Retrieves a paginated and optionally filtered list of customer stats.
     * Phone masking is handled internally based on the current authenticated user's
     * role.
     */
    Page<CustomerStatsDTO> getCustomers(String search, Integer page, Integer size);

    /**
     * Retrieves an unpaginated and optionally filtered list of customer stats.
     */
    List<CustomerStatsDTO> getCustomers(String search);

    /**
     * Retrieves customer history. Masking is applied automatically based on role.
     */
    CustomerHistoryDTO getCustomerHistory(String phoneNumber);
}
