package com.madhurgram.productservice.logistics.provider;

public interface ShiprocketAuthService {
    String getToken();
    String refreshToken();
}