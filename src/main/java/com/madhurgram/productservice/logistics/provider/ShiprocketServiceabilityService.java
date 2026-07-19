package com.madhurgram.productservice.logistics.provider;

import com.madhurgram.productservice.logistics.dto.ServiceabilityResultDto;

public interface ShiprocketServiceabilityService {
    ServiceabilityResultDto checkServiceability(String deliveryPincode);
}