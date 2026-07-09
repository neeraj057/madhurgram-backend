package com.madhurgram.productservice.order.event;

import com.madhurgram.productservice.order.entity.Order;
import org.springframework.context.ApplicationEvent;

public class OrderConfirmedEvent extends ApplicationEvent {
    private final Order order;

    public OrderConfirmedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
