package com.madhurgram.productservice.admin.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.madhurgram.productservice.common.util.DataMaskingUtil;
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

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final OrderRepository orderRepository;
    private final CustomerMapper customerMapper;

    public AdminCustomerServiceImpl(OrderRepository orderRepository, CustomerMapper customerMapper) {
        this.orderRepository = orderRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerStatsDTO> getCustomers(String search, Integer page, Integer size) {
        log.info("Computing paginated dashboard statistics: search='{}', page={}, size={}", search, page, size);
        
        List<CustomerStatsDTO> allProcessed = processAllCustomers(search);
        
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allProcessed.size());
        
        List<CustomerStatsDTO> content = (start < allProcessed.size()) 
                ? allProcessed.subList(start, end) 
                : List.of();
        
        return new PageImpl<>(content, pageable, allProcessed.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerStatsDTO> getCustomers(String search) {
        log.info("Computing unpaginated dashboard statistics: search='{}'", search);
        return processAllCustomers(search);
    }

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
            return null; // Controller will handle 404
        }

        CustomerHistoryDTO history = customerMapper.toHistoryDTO(phone.trim(), orders);
        
        if (!isSuperAdmin()) {
            return new CustomerHistoryDTO(
                    history.name(),
                    DataMaskingUtil.maskPhoneNumber(history.phoneNumber()),
                    history.orderHistory(),
                    history.totalSpent(),
                    history.totalOrders());
        }

        return history;
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private List<CustomerStatsDTO> processAllCustomers(String search) {
        List<Order> allOrders = orderRepository.findAllWithItems();
        
        List<CustomerStatsDTO> stats = allOrders.stream()
                .filter(order -> order.getPhoneNumber() != null && !order.getPhoneNumber().trim().isEmpty())
                .collect(Collectors.groupingBy(Order::getPhoneNumber))
                .entrySet().stream()
                .map(entry -> customerMapper.toStatsDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
                
        boolean isSuperAdmin = isSuperAdmin();
        if (!isSuperAdmin) {
            stats = maskPhoneNumbers(stats);
        }

        return filterAndSort(stats, search);
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> ROLE_SUPER_ADMIN.equals(a.getAuthority())
                        || SUPER_ADMIN.equals(a.getAuthority()));
    }

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