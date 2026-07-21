package com.madhurgram.productservice.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;
    
    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^(?:\\+91|91)?[6-9]\\d{9}$", message = "Invalid phone number format")
    private String phoneNumber;
    
    private String fullName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private List<AddressDTO> addresses;

    /**
     * Helper method to safely log phone numbers without exposing PII.
     */
    public String getMaskedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        return "******" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}