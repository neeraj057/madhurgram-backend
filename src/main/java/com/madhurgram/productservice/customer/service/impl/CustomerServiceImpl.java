package com.madhurgram.productservice.customer.service.impl;

import com.madhurgram.productservice.customer.dto.AddressDTO;
import com.madhurgram.productservice.customer.dto.CustomerDTO;
import com.madhurgram.productservice.customer.entity.Address;
import com.madhurgram.productservice.customer.entity.Customer;
import com.madhurgram.productservice.customer.repository.CustomerRepository;
import com.madhurgram.productservice.customer.service.CustomerService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing customer profiles, registration, 
 * and delivery addresses.
 */
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Constructor injection for CustomerServiceImpl.
     *
     * @param customerRepository customer repository dependency
     */
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Resolves the profile details of a customer.
     * Registers a new customer profile on-the-fly if not already present.
     *
     * @param phoneNumber customer phone number
     * @return resolved customer details DTO
     */
    @Override
    @Transactional
    public CustomerDTO getCustomerProfile(String phoneNumber) {
        log.info("Request: fetch customer profile for phone: '{}'", phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Profile fetch failed: phone parameter is blank");
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }

        String cleanPhone = phoneNumber.trim();

        Customer customer = customerRepository.findByPhoneNumberWithAddresses(cleanPhone)
                .orElseGet(() -> {
                    log.info("Customer record not found. Auto-registering profile for phone: '{}'", cleanPhone);
                    return customerRepository.save(Customer.builder()
                            .phoneNumber(cleanPhone)
                            .build());
                });

        log.info("Customer profile successfully resolved for phone: '{}'", cleanPhone);
        return mapToCustomerDTO(customer);
    }

    /**
     * Appends a new delivery address to a customer's active profile.
     * Toggles existing default address markers to maintain single-default rule.
     *
     * @param phoneNumber customer phone identifier
     * @param addressDTO  address attributes payload DTO
     * @return updated customer details DTO
     */
    @Override
    @Transactional
    public CustomerDTO addAddressToProfile(String phoneNumber, AddressDTO addressDTO) {
        log.info("Adding new delivery address to customer profile with phone: '{}'", phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }
        if (addressDTO == null) {
            throw new IllegalArgumentException("Address payload cannot be null.");
        }

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> {
                    log.warn("Address append failed: customer profile not found for phone: '{}'", phoneNumber);
                    return new IllegalArgumentException("Customer not found for phone number: " + phoneNumber);
                });

        // 🛑 Enforce maximum address limit of 5
        if (customer.getAddresses().size() >= 5) {
            log.warn("Address append failed: customer profile with phone '{}' reached max limit of 5 addresses", phoneNumber);
            throw new IllegalArgumentException("You cannot save more than 5 delivery addresses.");
        }

        Address newAddress = Address.builder()
                .addressType(addressDTO.getAddressType())
                .fullAddress(addressDTO.getFullAddress())
                .city(addressDTO.getCity())
                .state(addressDTO.getState())
                .pincode(addressDTO.getPincode())
                .isDefault(addressDTO.getIsDefault() != null && addressDTO.getIsDefault())
                .latitude(addressDTO.getLatitude())
                .longitude(addressDTO.getLongitude())
                .build();

        if (Boolean.TRUE.equals(newAddress.getIsDefault())) {
            log.info("New address is set as default. Clearing default flag from other addresses for customer ID: {}", customer.getId());
            customer.getAddresses().forEach(addr -> addr.setIsDefault(false));
        }

        customer.addAddress(newAddress);
        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Address successfully added to customer ID: {}", updatedCustomer.getId());
        return mapToCustomerDTO(updatedCustomer);
    }

    /**
     * Deletes a delivery address from a customer's active profile.
     *
     * @param phoneNumber customer phone identifier
     * @param addressId   ID of the address to delete
     * @return updated customer details DTO
     */
    @Override
    @Transactional
    public CustomerDTO deleteAddressFromProfile(String phoneNumber, Long addressId) {
        log.info("Request: delete address with ID: {} for customer phone: '{}'", addressId, phoneNumber);

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty.");
        }
        if (addressId == null) {
            throw new IllegalArgumentException("Address ID cannot be null.");
        }

        Customer customer = customerRepository.findByPhoneNumber(phoneNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for phone number: " + phoneNumber));

        // Find the address to delete from the customer's addresses
        Address addressToDelete = customer.getAddresses().stream()
                .filter(addr -> addr.getId().equals(addressId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address with ID " + addressId + " not found for this customer."));

        boolean wasDefault = Boolean.TRUE.equals(addressToDelete.getIsDefault());

        // Remove the address from the customer's collection
        customer.getAddresses().remove(addressToDelete);
        addressToDelete.setCustomer(null);

        // If the deleted address was marked as default, promote another address to default (if available)
        if (wasDefault && !customer.getAddresses().isEmpty()) {
            customer.getAddresses().get(0).setIsDefault(true);
            log.info("Deleted address was default. Promoted address ID: {} as new default for customer ID: {}",
                    customer.getAddresses().get(0).getId(), customer.getId());
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Address ID: {} successfully deleted from customer ID: {}", addressId, updatedCustomer.getId());
        return mapToCustomerDTO(updatedCustomer);
    }

    /**
     * Converts a Customer entity to a CustomerDTO.
     */
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
                        .latitude(addr.getLatitude())
                        .longitude(addr.getLongitude())
                        .build()).toList())
                .build();
    }
}