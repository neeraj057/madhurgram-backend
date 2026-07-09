package com.madhurgram.productservice.customer.dto;

import com.madhurgram.productservice.customer.entity.AddressType;
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
    private Double latitude;
    private Double longitude;
}