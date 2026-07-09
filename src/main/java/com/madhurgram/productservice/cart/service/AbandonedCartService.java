package com.madhurgram.productservice.cart.service;

import com.madhurgram.productservice.cart.dto.CartUpdateRequest;
import com.madhurgram.productservice.cart.entity.AbandonedCart;

import java.util.List;
import java.util.Optional;

public interface AbandonedCartService {

    AbandonedCart updateCart(CartUpdateRequest request);

    Optional<AbandonedCart> getCartToRecover(String phoneNumber);

    List<AbandonedCart> getAbandonedCarts(int minutesAgo);

    void markAsRecovered(String phoneNumber);

    boolean isAutoRecoveryEnabled();

    void setAutoRecoveryEnabled(boolean enabled);

    void sendAutomatedReminders();

    /**
     * Purges expired unrecovered abandoned carts from database.
     */
    void purgeExpiredCarts();

    /**
     * Manually deletes an abandoned cart by its ID.
     */
    void deleteAbandonedCart(Long id);
}
