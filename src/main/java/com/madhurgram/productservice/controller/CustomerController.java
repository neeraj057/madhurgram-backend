package com.madhurgram.productservice.controller;

import com.madhurgram.productservice.dto.AddressDTO;
import com.madhurgram.productservice.dto.CustomerDTO;
import com.madhurgram.productservice.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;

    // SOLID - Dependency Injection via Constructor
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 🔍 GET: कस्टमर की प्रोफाइल और उसके सारे सेव्ड एड्रेस फेच करने के लिए।
     * अगर कस्टमर पहली बार आ रहा है, तो सर्विस लेयर उसे ऑटो-रजिस्टर कर लेगी।
     */
    @GetMapping("/{phone}")
    public ResponseEntity<CustomerDTO> getCustomerProfile(@PathVariable String phone) {
        // Guard Clause
        if (phone == null || phone.trim().length() < 10) {
            throw new IllegalArgumentException("Invalid phone number format. Must be at least 10 digits.");
        }
        
        CustomerDTO profile = customerService.getCustomerProfile(phone.trim());
        return ResponseEntity.ok(profile); // 200 OK
    }

    /**
     * ➕ POST: कस्टमर की प्रोफाइल में एक नया डिलीवरी एड्रेस जोड़ने के लिए।
     */
    @PostMapping("/{phone}/addresses")
    public ResponseEntity<CustomerDTO> addAddressToProfile(
            @PathVariable String phone,
            @RequestBody AddressDTO addressDTO) {

        // Phone Validation Guard
        if (phone == null || phone.trim().length() < 10) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        
        // Payload Validation Guard
        if (addressDTO.getFullAddress() == null || addressDTO.getPincode() == null) {
            throw new IllegalArgumentException("Full address and Pincode are mandatory for delivery.");
        }
        if (addressDTO.getAddressType() == null) {
            throw new IllegalArgumentException("Address Type (HOME, OFFICE, OTHER) must be specified.");
        }

        // सर्विस कॉल
        CustomerDTO updatedProfile = customerService.addAddressToProfile(phone.trim(), addressDTO);
        
        // 201 CREATED स्टेटस के साथ अपडेटेड प्रोफाइल रिटर्न करो
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedProfile);
    }
}