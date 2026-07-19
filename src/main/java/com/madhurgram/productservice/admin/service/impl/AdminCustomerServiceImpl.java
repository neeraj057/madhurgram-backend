package com.madhurgram.productservice.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import com.madhurgram.productservice.customer.dto.CustomerHistoryDTO;
import com.madhurgram.productservice.customer.dto.CustomerStatsDTO;
import com.madhurgram.productservice.customer.mapper.CustomerMapper;
import com.madhurgram.productservice.order.entity.Order;
import com.madhurgram.productservice.order.repository.OrderRepository;
import com.madhurgram.productservice.admin.service.AdminCustomerService;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for managing admin customer accounts, 
 * aggregating order counts, and analyzing purchase histories.
 */
@Slf4j
@Service
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;

    /**
     * Constructor injection for AdminCustomerServiceImpl.
     *
     * @param orderRepository order database access
     * @param customerMapper  customer mapper instance
     */
    public AdminCustomerServiceImpl(OrderRepository orderRepository, CustomerMapper customerMapper) {
        this.orderRepository = orderRepository;
        this.customerMapper = customerMapper;
    }

    /**
     * Retrieves the history of all orders associated with a customer phone.
     *
     * @param phone the customer's phone number
     * @return the customer history DTO
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerHistoryDTO getCustomerHistory(String phone) {
        log.info("Fetching customer order history for phone: '{}'", phone);
        
        if (phone == null || phone.trim().isEmpty()) {
            log.warn("Failed to fetch customer history: Phone number parameter is blank");
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        List<Order> orders = orderRepository.findByPhoneNumber(phone.trim());
        if (orders.isEmpty()) {
            log.warn("Customer history lookup failed: No orders found for phone: '{}'", phone);
            throw new IllegalArgumentException("No order records found for the customer phone number: " + phone);
        }

        CustomerHistoryDTO history = customerMapper.toHistoryDTO(phone.trim(), orders);
        log.info("Successfully retrieved {} order(s) for customer phone: '{}'", orders.size(), phone);
        return history;
    }

    /**
     * Aggregates purchase statistics (spend, frequency, segment, favorite product) 
     * for all customers.
     * 
     * <p><b>Scalability Note:</b> In high-scale deployments, fetching all orders 
     * in-memory for grouping can cause substantial memory consumption. Consider 
     * native pagination or caching customer metrics if the order count grows extremely large.
     *
     * @return a list of customer statistics DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerStatsDTO> getAllCustomerStats() {
        log.info("Computing dashboard statistics for all customers from database orders");
        
        List<Order> allOrders = orderRepository.findAllWithItems();
        log.info("Loaded {} total order(s) from database for customer analysis", allOrders.size());

        List<CustomerStatsDTO> stats = allOrders.stream()
                .filter(order -> order.getPhoneNumber() != null && !order.getPhoneNumber().trim().isEmpty())
                .collect(Collectors.groupingBy(Order::getPhoneNumber))
                .entrySet().stream()
                .map(entry -> customerMapper.toStatsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.info("Successfully aggregated statistics for {} unique customer(s)", stats.size());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerStatsDTO> getAllCustomerStats(Pageable pageable) {
        log.info("Computing paginated dashboard statistics: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<String> phonePage = orderRepository.findDistinctPhoneNumbers(pageable);
        List<String> phoneNumbers = phonePage.getContent();
        
        if (phoneNumbers.isEmpty()) {
            log.info("No customers found for page {}", pageable.getPageNumber());
            return new PageImpl<>(List.of(), pageable, phonePage.getTotalElements());
        }
        
        List<Order> orders = orderRepository.findOrdersWithItemsByPhoneNumbers(phoneNumbers);
        
        List<CustomerStatsDTO> stats = orders.stream()
                .filter(order -> order.getPhoneNumber() != null && !order.getPhoneNumber().trim().isEmpty())
                .collect(Collectors.groupingBy(Order::getPhoneNumber))
                .entrySet().stream()
                .map(entry -> customerMapper.toStatsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
                
        List<CustomerStatsDTO> sortedStats = phoneNumbers.stream()
                .map(phone -> stats.stream()
                        .filter(s -> s.phoneNumber().equals(phone))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        log.info("Successfully aggregated paginated statistics for {} customer(s)", sortedStats.size());
        return new PageImpl<>(sortedStats, pageable, phonePage.getTotalElements());
    }
}