package com.madhurgram.productservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.madhurgram.productservice.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.dto.CustomerStatsDTO;
import com.madhurgram.productservice.dto.OrderSummaryDTO;
import com.madhurgram.productservice.entity.Order;
import com.madhurgram.productservice.mapper.CustomerMapper;
import com.madhurgram.productservice.repository.OrderRepository;
import com.madhurgram.productservice.service.AdminCustomerService;
@Service
public class AdminCustomerServiceImpl implements AdminCustomerService {
    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper; // Mapper इंजेक्ट किया

    public AdminCustomerServiceImpl(OrderRepository orderRepository, CustomerMapper customerMapper) {
        this.orderRepository = orderRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerHistoryDTO getCustomerHistory(String phone) {
        List<Order> orders = orderRepository.findByPhoneNumber(phone);
        if (orders.isEmpty()) throw new RuntimeException("Customer not found");

        return customerMapper.toHistoryDTO(phone, orders);
    }

    @Override
    public List<CustomerStatsDTO> getAllCustomerStats() {
        return orderRepository.findAll().stream()
            .collect(Collectors.groupingBy(Order::getPhoneNumber))
            .entrySet().stream()
            .map(entry -> customerMapper.toStatsDTO(entry.getKey(), entry.getValue())) // Mapper यूज़ किया
            .collect(Collectors.toList());
    }
}