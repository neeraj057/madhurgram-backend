package com.madhurgram.productservice.logistics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PincodeSlaResponseDto {
    private boolean available;
    private String tier;
    private String sla;
    private Boolean cod;
    private String courier;
    private String message;
    private String disclaimer;
    private String location;
}