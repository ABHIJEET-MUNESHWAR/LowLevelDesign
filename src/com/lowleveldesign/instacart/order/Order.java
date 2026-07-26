package com.lowleveldesign.instacart.order;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Aggregate representing a customer's order at a single store. */
public class Order {
    private final String orderId;
    private final String customerId;
    private final String storeId;
    private final List<OrderItem> items;
    private final List<String> reservationIds = new ArrayList<>();
    private OrderStatus status;

    public Order(String customerId, String storeId, List<OrderItem> items) {
        this.orderId = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.storeId = storeId;
        this.items = items;
        this.status = OrderStatus.PENDING;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getStoreId() {
        return storeId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public List<String> getReservationIds() {
        return reservationIds;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
