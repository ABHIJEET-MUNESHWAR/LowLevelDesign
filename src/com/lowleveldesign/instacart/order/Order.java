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

    /**
     * Creates a new order in {@code PENDING} status with a freshly generated id and no
     * reservations yet attached.
     *
     * @param customerId the customer placing the order
     * @param storeId    the store the order is fulfilled from
     * @param items      the requested line items
     */
    public Order(String customerId, String storeId, List<OrderItem> items) {
        this.orderId = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.storeId = storeId;
        this.items = items;
        this.status = OrderStatus.PENDING;
    }

    /**
     * Returns the unique identifier of this order.
     *
     * @return the order id
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Returns the id of the customer who placed this order.
     *
     * @return the customer id
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the id of the store this order is fulfilled from.
     *
     * @return the store id
     */
    public String getStoreId() {
        return storeId;
    }

    /**
     * Returns the requested line items for this order.
     *
     * @return the list of order items
     */
    public List<OrderItem> getItems() {
        return items;
    }

    /**
     * Returns the ids of the reservations created for this order's line items, in the order they
     * were made.
     *
     * @return the mutable list of reservation ids
     */
    public List<String> getReservationIds() {
        return reservationIds;
    }

    /**
     * Returns the current lifecycle status of this order.
     *
     * @return the order status
     */
    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Updates the lifecycle status of this order (e.g. to CONFIRMED or CANCELLED).
     *
     * @param status the new status
     */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
