package com.madhurgram.productservice.dto;

import com.madhurgram.productservice.entity.AddressType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;
    private AddressType addressType;
    private String fullAddress;
    private String city;
    private String state;
    private String pincode;
    private Boolean isDefault;
}