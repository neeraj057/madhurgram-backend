package com.madhurgram.productservice.logistics.service;

import com.madhurgram.productservice.logistics.dto.AdminPincodeSettingsDto;
import com.madhurgram.productservice.logistics.dto.PincodeSlaResponseDto;
import com.madhurgram.productservice.logistics.dto.ShiprocketSettingsDto;

public interface PincodeSlaService {
    PincodeSlaResponseDto checkPincode(String pincode);
    AdminPincodeSettingsDto getAdminPincodeSettings();
    AdminPincodeSettingsDto updateAdminPincodeSettings(AdminPincodeSettingsDto payload);
    ShiprocketSettingsDto getShiprocketSettings();
    ShiprocketSettingsDto updateShiprocketSettings(ShiprocketSettingsDto payload);
}