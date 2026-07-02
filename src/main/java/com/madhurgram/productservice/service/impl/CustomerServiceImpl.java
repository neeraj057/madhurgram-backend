package com.madhurgram.productservice.service.impl;

import com.madhurgram.productservice.dto.AddressDTO;
import com.madhurgram.productservice.dto.CustomerDTO;
import com.madhurgram.productservice.entity.Address;
import com.madhurgram.productservice.entity.Customer;
import com.madhurgram.productservice.repository.CustomerRepository;
import com.madhurgram.productservice.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    // SOLID - Dependency Inversion (Constructor Injection)
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerDTO getCustomerProfile(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        String cleanPhone = phoneNumber.trim();

        // 🔄 On-the-fly Registration: Agar customer database me nahi hai, to naya profile create karke save kar do
        Customer customer = customerRepository.findByPhoneNumberWithAddresses(cleanPhone)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .phoneNumber(cleanPhone)
                        .build()));

        return mapToCustomerDTO(customer);
    }

    @Override
    @Transactional
    public CustomerDTO addAddressToProfile(String phoneNumber, AddressDTO addressDTO) {
        Customer customer = customerRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> new RuntimeException("customer not found for phone number: " + phoneNumber));

        // Naya Address Entity entity instantiation
        Address newAddress = Address.builder()
                .addressType(addressDTO.getAddressType())
                .fullAddress(addressDTO.getFullAddress())
                .city(addressDTO.getCity())
                .state(addressDTO.getState())
                .pincode(addressDTO.getPincode())
                .isDefault(addressDTO.getIsDefault() != null && addressDTO.getIsDefault())
                .build();

        // 🛡️ Business Rule: Agar ye naya address DEFAULT mark hua hai, to baaki sabko false karo
        if (Boolean.TRUE.equals(newAddress.getIsDefault())) {
            customer.getAddresses().forEach(addr -> addr.setIsDefault(false));
        }

        // Bi-directional state sync helper call
        customer.addAddress(newAddress);

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToCustomerDTO(updatedCustomer);
    }

    // 🔄 Entity to DTO Conversion Engine
    private CustomerDTO mapToCustomerDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .phoneNumber(customer.getPhoneNumber())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .addresses(customer.getAddresses().stream().map(addr -> AddressDTO.builder()
                        .id(addr.getId())
                        .addressType(addr.getAddressType())
                        .fullAddress(addr.getFullAddress())
                        .city(addr.getCity())
                        .state(addr.getState())
                        .pincode(addr.getPincode())
                        .isDefault(addr.getIsDefault())
                        .build()).toList())
                .build();
    }
}