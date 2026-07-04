package com.madhurgram.productservice.admin.service;

import java.util.List;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;

public interface AdminCustomerService {
    // एडमिन के लिए पूरी लिस्ट
    List<CustomerStatsDTO> getAllCustomerStats();

    // किसी खास कस्टमर की हिस्ट्री
    CustomerHistoryDTO getCustomerHistory(String phoneNumber);

}
