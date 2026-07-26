package com.lowleveldesign.instacart.order;

import com.lowleveldesign.instacart.inventory.InsufficientStockException;
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

    public OrderService(InventoryManager inventoryManager) {
        this.inventoryManager = inventoryManager;
    }

    public Order placeOrder(String customerId, String storeId, List<OrderItem> items) {
        Order order = new Order(customerId, storeId, items);
        try {
            for (OrderItem item : items) {
                Reservation reservation = inventoryManager.reserveStock(
                        storeId, item.getProductId(), item.getQuantity(), RESERVATION_TTL_SECONDS);
                order.getReservationIds().add(reservation.getReservationId());
            }
        } catch (InsufficientStockException e) {
            // Roll back any reservations already made for this order before propagating.
            rollback(order);
            order.setStatus(OrderStatus.CANCELLED);
            throw e;
        }
        return order;
    }

    public void checkout(Order order) {
        for (String reservationId : order.getReservationIds()) {
            inventoryManager.confirmReservation(reservationId);
        }
        order.setStatus(OrderStatus.CONFIRMED);
    }

    public void cancelOrder(Order order) {
        rollback(order);
        order.setStatus(OrderStatus.CANCELLED);
    }

    private void rollback(Order order) {
        for (String reservationId : order.getReservationIds()) {
            try {
                inventoryManager.cancelReservation(reservationId);
            } catch (RuntimeException ignored) {
                // Already confirmed/cancelled/expired - nothing to undo.
            }
        }
    }
}
