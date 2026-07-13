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
@RequestMapping("/api/customers")
@Tag(name = "Customer", description = "Endpoints for managing buyer profiles and delivery addresses")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Constructor injection for CustomerController.
     *
     * @param customerService customer service management
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Retrieves the profile, including active delivery addresses, for a customer.
     * Registers a new customer implicitly if they do not exist yet.
     *
     * @param phone validated buyer phone number
     * @return the profile DTO
     */
    @GetMapping("/{phone}")
    @Operation(summary = "Get or register customer profile", description = "Fetches details of a customer profile. Auto-registers the customer if not found.")
    public ResponseEntity<CustomerDTO> getCustomerProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone) {
        log.info("Request: get customer profile for phone='{}'", phone);
        
        CustomerDTO profile = customerService.getCustomerProfile(phone.trim());
        log.info("Returning profile for phone='{}' (name='{}')", phone, profile.getFullName());
        return ResponseEntity.ok(profile);
    }

    /**
     * Appends a new delivery address to a customer's profile.
     *
     * @param phone      validated buyer phone number
     * @param addressDTO delivery address fields payload
     * @return the updated customer profile containing the new address
     */
    @PostMapping("/{phone}/addresses")
    @Operation(summary = "Add address to profile", description = "Adds a delivery location/address under a customer's phone identification.")
    public ResponseEntity<CustomerDTO> addAddressToProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @RequestBody AddressDTO addressDTO) {
        log.info("Request: add address type {} to profile phone='{}'", addressDTO.getAddressType(), phone);

        if (addressDTO.getFullAddress() == null || addressDTO.getPincode() == null) {
            log.warn("Add address failed: missing fullAddress or pincode");
            throw new IllegalArgumentException("Full address and Pincode are mandatory for delivery.");
        }
        if (addressDTO.getAddressType() == null) {
            log.warn("Add address failed: missing addressType");
            throw new IllegalArgumentException("Address Type (HOME, OFFICE, OTHER) must be specified.");
        }

        CustomerDTO updatedProfile = customerService.addAddressToProfile(phone.trim(), addressDTO);
        log.info("Address successfully added to profile phone='{}'", phone);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedProfile);
    }

    /**
     * Deletes a delivery address from a customer's active profile.
     *
     * @param phone     validated buyer phone number
     * @param addressId ID of the address to delete
     * @return the updated customer profile containing remaining addresses
     */
    @DeleteMapping("/{phone}/addresses/{addressId}")
    @Operation(summary = "Delete address from profile", description = "Removes a specific delivery address from a customer's profile.")
    public ResponseEntity<CustomerDTO> deleteAddressFromProfile(
            @PathVariable
            @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format. Must be a valid 10-digit Indian mobile number optionally prefixed with +91 or 91.")
            String phone,
            @PathVariable Long addressId) {
        log.info("Request: delete address ID {} for profile phone='{}'", addressId, phone);

        CustomerDTO updatedProfile = customerService.deleteAddressFromProfile(phone.trim(), addressId);
        log.info("Address ID {} successfully deleted from profile phone='{}'", addressId, phone);
        return ResponseEntity.ok(updatedProfile);
    }
}