package com.madhurgram.productservice.logistics.dto;

import lombok.Data;

@Data
public class AdminPincodeSettingsDto {
    private String pincodeSlaLocal;
    private String pincodeSlaRegional;
    private String pincodeSlaNational;
}