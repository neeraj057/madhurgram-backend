package com.madhurgram.productservice.customer.controller;

import com.madhurgram.productservice.customer.dto.AddressDTO;
import com.madhurgram.productservice.customer.dto.CustomerDTO;
import com.madhurgram.productservice.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;

/**
 * Controller for customer accounts and delivery addresses management.
 * 
 * <p>Handles auto-registration of new customers when querying profile endpoints.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer (v1)", description = "Endpoints for managing buyer profiles and delivery addresses")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return "******" + phone.substring(phone.length() - 4);
    }

    @GetMapping("/{phone}")
    @Operation(summary = "Get or register customer profile", description = "Fetches details of a customer profile. Auto-registers the customer if not found.")
    public ResponseEntity<CustomerDTO> getCustomerProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone) {
        String cleanPhone = phone.trim();
        log.info("Request: get customer profile for phone='{}'", maskPhone(cleanPhone));
        
        CustomerDTO profile = customerService.getCustomerProfile(cleanPhone);
        log.info("Returning profile for phone='{}' (name='{}')", maskPhone(cleanPhone), profile.getFullName());
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{phone}/addresses")
    @Operation(summary = "Add address to profile", description = "Adds a delivery location/address under a customer's phone identification.")
    public ResponseEntity<CustomerDTO> addAddressToProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @RequestBody AddressDTO addressDTO) {
        String cleanPhone = phone.trim();
        log.info("Request: add address type {} to profile phone='{}'", addressDTO.getAddressType(), maskPhone(cleanPhone));

        if (addressDTO.getFullAddress() == null || addressDTO.getPincode() == null) {
            log.warn("Add address failed: missing fullAddress or pincode for phone='{}'", maskPhone(cleanPhone));
            throw new IllegalArgumentException("Full address and Pincode are mandatory for delivery.");
        }
        if (addressDTO.getAddressType() == null) {
            log.warn("Add address failed: missing addressType for phone='{}'", maskPhone(cleanPhone));
            throw new IllegalArgumentException("Address Type (HOME, OFFICE, OTHER) must be specified.");
        }

        CustomerDTO updatedProfile = customerService.addAddressToProfile(cleanPhone, addressDTO);
        log.info("Address successfully added to profile phone='{}'", maskPhone(cleanPhone));
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedProfile);
    }

    @DeleteMapping("/{phone}/addresses/{addressId}")
    @Operation(summary = "Delete address from profile", description = "Removes a specific delivery address from a customer's profile.")
    public ResponseEntity<CustomerDTO> deleteAddressFromProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @PathVariable Long addressId) {
        String cleanPhone = phone.trim();
        log.info("Request: delete address ID {} for profile phone='{}'", addressId, maskPhone(cleanPhone));

        CustomerDTO updatedProfile = customerService.deleteAddressFromProfile(cleanPhone, addressId);
        log.info("Address ID {} successfully deleted from profile phone='{}'", addressId, maskPhone(cleanPhone));
        return ResponseEntity.ok(updatedProfile);
    }
}