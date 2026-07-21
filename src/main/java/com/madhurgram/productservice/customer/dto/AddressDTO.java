package com.madhurgram.productservice.customer.dto;

import com.madhurgram.productservice.customer.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;
    
    @NotNull(message = "Address type is mandatory")
    private AddressType addressType;
    
    @NotBlank(message = "Full address is mandatory")
    private String fullAddress;
    
    @NotBlank(message = "City is mandatory")
    private String city;
    
    @NotBlank(message = "State is mandatory")
    private String state;
    
    @NotBlank(message = "Pincode is mandatory")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid Indian pincode format")
    private String pincode;
    
    private Boolean isDefault;
    private Double latitude;
    private Double longitude;
}