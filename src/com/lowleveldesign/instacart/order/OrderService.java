package com.lowleveldesign.instacart.order;

import com.lowleveldesign.instacart.exception.InstacartException;
import com.lowleveldesign.instacart.exception.InvalidOrderException;
import com.lowleveldesign.instacart.exception.InvalidOrderStateException;
import com.lowleveldesign.instacart.inventory.InventoryManager;
import com.lowleveldesign.instacart.inventory.Reservation;

import java.util.List;

/**
 * Orchestrates the order lifecycle on top of InventoryManager:
 *  1. placeOrder  -> reserve stock for every line item (all-or-nothing)
 *  2. checkout    -> confirm all reservations, permanently deducting stock
 *  3. cancelOrder -> release all reservations back to the available pool
 */
public class OrderService {

    private static final long RESERVATION_TTL_SECONDS = 15 * 60; // 15-minute cart hold

    private final InventoryManager inventoryManager;

    /**
     * Creates an order service backed by the given inventory manager.
     *
     * @param inventoryManager the inventory manager used to reserve/confirm/cancel stock
     */
    public OrderService(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    /**
     * Places a new order: reserves stock for every requested line item. The reservation is
     * all-or-nothing — if any single line item cannot be reserved, every reservation already made
     * for this order is rolled back and the order is marked CANCELLED before the failure is
     * propagated to the caller.
     *
     * @param customerId the customer placing the order
     * @param storeId    the store to fulfill the order from
     * @param items      the requested line items; must not be null or empty
     * @return the order, in PENDING status with stock held, if every item was reserved
     * @throws InvalidOrderException      if {@code items} is null or empty
     * @throws InsufficientStockException if any line item cannot be fully reserved
     * @throws ProductNotTrackedException if any requested product is not stocked at the store
     */
    public Order placeOrder(String customerId, String storeId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one line item");
        }
        Order order = new Order(customerId, storeId, items);
        try {
            for (OrderItem item : items) {
                Reservation reservation = inventoryManager.reserveStock(
                        storeId, item.getProductId(), item.getQuantity(), RESERVATION_TTL_SECONDS);
                order.getReservationIds().add(reservation.getReservationId());
            }
        } catch (InstacartException e) {
            // Roll back any reservations already made for this order before propagating.
            rollback(order);
            order.setStatus(OrderStatus.CANCELLED);
            throw e;
        }
        return order;
    }

    /**
     * Finalizes a PENDING order at checkout: every reservation held for the order is confirmed,
     * permanently deducting stock, and the order is marked CONFIRMED.
     *
     * @param order the order to check out
     * @throws InvalidOrderStateException if the order is not currently PENDING
     */
    public void checkout(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(order.getOrderId(), order.getStatus(), "checkout");
        }
        for (String reservationId : order.getReservationIds()) {
            inventoryManager.confirmReservation(reservationId);
        }
        order.setStatus(OrderStatus.CONFIRMED);
    }

    /**
     * Cancels a PENDING order: every reservation held for the order is released back to the
     * available pool, and the order is marked CANCELLED.
     *
     * @param order the order to cancel
     * @throws InvalidOrderStateException if the order is not currently PENDING
     */
    public void cancelOrder(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(order.getOrderId(), order.getStatus(), "cancel");
        }
        rollback(order);
        order.setStatus(OrderStatus.CANCELLED);
    }

    /**
     * Releases every reservation currently attached to an order, ignoring failures for
     * reservations that are already confirmed, cancelled, or expired since there is nothing left
     * to undo for those.
     *
     * @param order the order whose reservations should be released
     */
    private void rollback(Order order) {
        for (String reservationId : order.getReservationIds()) {
            try {
                inventoryManager.cancelReservation(reservationId);
            } catch (InstacartException ignored) {
                // Already confirmed/cancelled/expired - nothing to undo.
            }
        }
    }
}
