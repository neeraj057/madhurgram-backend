package com.madhurgram.productservice.cart.service;

import com.madhurgram.productservice.cart.dto.AbandonedCartResponse;
import com.madhurgram.productservice.cart.dto.CartUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface AbandonedCartService {

    AbandonedCartResponse updateCart(CartUpdateRequest request);

    Optional<AbandonedCartResponse> getCartToRecover(String phoneNumber);

    List<AbandonedCartResponse> getAbandonedCarts(int minutesAgo);

    void markAsRecovered(String phoneNumber);

    boolean isAutoRecoveryEnabled();

    void setAutoRecoveryEnabled(boolean enabled);

    void sendAutomatedReminders();

    void purgeExpiredCarts();

    void deleteAbandonedCart(Long id);
}
