package com.madhurgram.productservice.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;
import com.madhurgram.productservice.customer.mapper.CustomerMapper;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.admin.service.AdminCustomerService;

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
        if (orders.isEmpty())
            throw new RuntimeException("Customer not found");

        return customerMapper.toHistoryDTO(phone, orders);
    }

    @Override
    public List<CustomerStatsDTO> getAllCustomerStats() {
        return orderRepository.findAllWithItems().stream()
                .collect(Collectors.groupingBy(Order::getPhoneNumber))
                .entrySet().stream()
                .map(entry -> customerMapper.toStatsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}