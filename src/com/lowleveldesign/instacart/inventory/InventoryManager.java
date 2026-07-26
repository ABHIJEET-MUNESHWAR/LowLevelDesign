package com.lowleveldesign.instacart.inventory;

import com.lowleveldesign.instacart.model.Product;
import com.lowleveldesign.instacart.model.Store;
import com.lowleveldesign.instacart.observer.InventoryObserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central, thread-safe facade over all store inventories. Singleton so every
 * part of the system (order flow, admin restock tools, background jobs)
 * shares one consistent view of stock.
 */
public final class InventoryManager {

    private static final InventoryManager INSTANCE = new InventoryManager();

    // key = storeId + "::" + productId
    private final Map<String, InventoryItem> inventory = new ConcurrentHashMap<>();
    private final Map<String, Reservation> reservations = new ConcurrentHashMap<>();
    private final List<InventoryObserver> observers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private InventoryManager() {
        // Sweep for expired reservations every 5 seconds so abandoned carts
        // return stock to the pool automatically.
        scheduler.scheduleAtFixedRate(this::releaseExpiredReservations, 5, 5, TimeUnit.SECONDS);
    }

    public static InventoryManager getInstance() {
        return INSTANCE;
    }

    private String key(String storeId, String productId) {
        return storeId + "::" + productId;
    }

    public void registerObserver(InventoryObserver observer) {
        observers.add(observer);
    }

    private void notifyIfLow(InventoryItem item) {
        if (item.isBelowThreshold()) {
            for (InventoryObserver observer : observers) {
                observer.onLowStock(item);
            }
        }
    }

    /** Onboards a product at a store with initial stock, or tops up an existing entry. */
    public void stockProduct(Store store, Product product, int quantity, int lowStockThreshold) {
        inventory.compute(key(store.getStoreId(), product.getProductId()), (k, existing) -> {
            if (existing == null) {
                return new InventoryItem(store, product, quantity, lowStockThreshold);
            }
            existing.addStock(quantity);
            return existing;
        });
    }

    /** Supplier restock for an already-tracked item. */
    public void restock(String storeId, String productId, int quantity) {
        InventoryItem item = getItem(storeId, productId);
        item.addStock(quantity);
    }

    public int getAvailableQuantity(String storeId, String productId) {
        return getItem(storeId, productId).getAvailableQuantity();
    }

    private InventoryItem getItem(String storeId, String productId) {
        InventoryItem item = inventory.get(key(storeId, productId));
        if (item == null) {
            throw new IllegalArgumentException("Product " + productId + " is not tracked at store " + storeId);
        }
        return item;
    }

    /**
     * Holds stock for a cart/checkout flow. Reservation expires automatically
     * after {@code ttlSeconds} if not confirmed or cancelled first.
     */
    public Reservation reserveStock(String storeId, String productId, int quantity, long ttlSeconds) {
        InventoryItem item = getItem(storeId, productId);
        if (!item.reserve(quantity)) {
            throw new InsufficientStockException(productId, storeId, quantity, item.getAvailableQuantity());
        }
        Reservation reservation = new Reservation(storeId, productId, quantity, ttlSeconds);
        reservations.put(reservation.getReservationId(), reservation);
        notifyIfLow(item);
        return reservation;
    }

    /** Finalizes a reservation at checkout: stock is permanently deducted. */
    public void confirmReservation(String reservationId) {
        Reservation reservation = requireActiveReservation(reservationId);
        InventoryItem item = getItem(reservation.getStoreId(), reservation.getProductId());
        item.confirm(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CONFIRMED);
    }

    /** Cancels a reservation (e.g. shopper removed item from cart): stock returns to available pool. */
    public void cancelReservation(String reservationId) {
        Reservation reservation = requireActiveReservation(reservationId);
        InventoryItem item = getItem(reservation.getStoreId(), reservation.getProductId());
        item.release(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    private Reservation requireActiveReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("Unknown reservation: " + reservationId);
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation " + reservationId + " is not active (status="
                    + reservation.getStatus() + ")");
        }
        return reservation;
    }

    private void releaseExpiredReservations() {
        for (Reservation reservation : reservations.values()) {
            if (reservation.isExpired()) {
                InventoryItem item = inventory.get(key(reservation.getStoreId(), reservation.getProductId()));
                if (item != null) {
                    item.release(reservation.getQuantity());
                }
                reservation.setStatus(ReservationStatus.EXPIRED);
            }
        }
    }

    public InventoryItem getInventoryItem(String storeId, String productId) {
        return getItem(storeId, productId);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
