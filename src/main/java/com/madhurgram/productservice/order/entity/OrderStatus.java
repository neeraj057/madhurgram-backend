package com.madhurgram.productservice.order.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /**
     * Checks if this status can transition to the given next status.
     * Implements the order state machine validations.
     */
    public boolean isValidTransition(OrderStatus nextStatus) {
        return switch (this) {
            case PENDING -> nextStatus == CONFIRMED || nextStatus == CANCELLED;
            case CONFIRMED -> nextStatus == SHIPPED || nextStatus == PENDING || nextStatus == CANCELLED;
            case SHIPPED -> nextStatus == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}