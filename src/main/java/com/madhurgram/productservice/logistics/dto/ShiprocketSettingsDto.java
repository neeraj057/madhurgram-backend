package com.madhurgram.productservice.logistics.dto;

import lombok.Data;

@Data
public class ShiprocketSettingsDto {
    private String shiprocketEnabled;
    private String shiprocketEmail;
    private String shiprocketPassword;
    private String shiprocketPasswordConfigured;
    private String shiprocketPickupPincode;
}