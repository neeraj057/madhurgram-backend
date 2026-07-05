package com.madhurgram.productservice.order.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    /**
     * Checks if this status can transition to the given next status.
     * Implements the order state machine validations.
     */
    public boolean isValidTransition(OrderStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED -> nextStatus == SHIPPED || nextStatus == CANCELLED;
            case SHIPPED -> nextStatus == OUT_FOR_DELIVERY || nextStatus == CANCELLED;
            case OUT_FOR_DELIVERY -> nextStatus == DELIVERED || nextStatus == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}