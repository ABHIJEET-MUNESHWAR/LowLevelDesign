package com.lowleveldesign.instacart.inventory;

import com.lowleveldesign.instacart.exception.InsufficientStockException;
import com.lowleveldesign.instacart.exception.InvalidReservationStateException;
import com.lowleveldesign.instacart.exception.ProductNotTrackedException;
import com.lowleveldesign.instacart.exception.ReservationNotFoundException;
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

    /**
     * Private constructor enforcing the singleton pattern; also starts the background sweep that
     * auto-releases expired reservations every 5 seconds so abandoned carts return stock to the
     * pool automatically.
     */
    private InventoryManager() {
        scheduler.scheduleAtFixedRate(this::releaseExpiredReservations, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * Returns the single shared instance of the inventory manager.
     *
     * @return the singleton {@code InventoryManager}
     */
    public static InventoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Builds the composite lookup key used to index inventory items by store and product.
     *
     * @param storeId   the store id
     * @param productId the product id
     * @return the combined key
     */
    private String key(String storeId, String productId) {
        return storeId + "::" + productId;
    }

    /**
     * Registers an observer to be notified whenever a reservation causes an item's available
     * quantity to drop at or below its low-stock threshold.
     *
     * @param observer the observer to register
     */
    public void registerObserver(InventoryObserver observer) {
        observers.add(observer);
    }

    /**
     * Notifies every registered observer if the given item's available quantity is currently at
     * or below its configured low-stock threshold.
     *
     * @param item the item to check and, if low, report to observers
     */
    private void notifyIfLow(InventoryItem item) {
        if (item.isBelowThreshold()) {
            for (InventoryObserver observer : observers) {
                observer.onLowStock(item);
            }
        }
    }

    /**
     * Onboards a product at a store with initial stock, or tops up the quantity of an existing
     * entry for that (store, product) pair.
     *
     * @param store             the store to stock
     * @param product           the product to stock
     * @param quantity          the quantity to add
     * @param lowStockThreshold the low-stock threshold to use if this is a new entry
     */
    public void stockProduct(Store store, Product product, int quantity, int lowStockThreshold) {
        inventory.compute(key(store.getStoreId(), product.getProductId()), (k, existing) -> {
            if (existing == null) {
                return new InventoryItem(store, product, quantity, lowStockThreshold);
            }
            existing.addStock(quantity);
            return existing;
        });
    }

    /**
     * Adds supplier-replenished stock to an already-tracked (store, product) pair.
     *
     * @param storeId   the store id
     * @param productId the product id
     * @param quantity  the quantity to add
     * @throws ProductNotTrackedException if the (store, product) pair has never been stocked
     */
    public void restock(String storeId, String productId, int quantity) {
        InventoryItem item = getItem(storeId, productId);
        item.addStock(quantity);
    }

    /**
     * Returns the quantity currently free to reserve for a (store, product) pair.
     *
     * @param storeId   the store id
     * @param productId the product id
     * @return the available quantity
     * @throws ProductNotTrackedException if the (store, product) pair has never been stocked
     */
    public int getAvailableQuantity(String storeId, String productId) {
        return getItem(storeId, productId).getAvailableQuantity();
    }

    /**
     * Looks up the tracked inventory item for a (store, product) pair.
     *
     * @param storeId   the store id
     * @param productId the product id
     * @return the tracked inventory item
     * @throws ProductNotTrackedException if the (store, product) pair has never been stocked
     */
    private InventoryItem getItem(String storeId, String productId) {
        InventoryItem item = inventory.get(key(storeId, productId));
        if (item == null) {
            throw new ProductNotTrackedException(productId, storeId);
        }
        return item;
    }

    /**
     * Holds stock for a cart/checkout flow. The reservation expires automatically after
     * {@code ttlSeconds} if it is never confirmed or cancelled, returning its stock to the
     * available pool. If the reservation pushes the item's available quantity at/below its
     * low-stock threshold, registered observers are notified.
     *
     * @param storeId    the store to reserve from
     * @param productId  the product to reserve
     * @param quantity   the quantity to hold
     * @param ttlSeconds seconds until this reservation auto-expires if untouched
     * @return the created, ACTIVE reservation
     * @throws ProductNotTrackedException  if the (store, product) pair has never been stocked
     * @throws InsufficientStockException if fewer than {@code quantity} units are available
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

    /**
     * Finalizes a reservation at checkout: the held stock is permanently deducted from both the
     * reserved and total counts, and the reservation is marked CONFIRMED.
     *
     * @param reservationId the id of the reservation to confirm
     * @throws ReservationNotFoundException      if no reservation exists with this id
     * @throws InvalidReservationStateException if the reservation is not currently ACTIVE
     */
    public void confirmReservation(String reservationId) {
        Reservation reservation = requireActiveReservation(reservationId);
        InventoryItem item = getItem(reservation.getStoreId(), reservation.getProductId());
        item.confirm(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CONFIRMED);
    }

    /**
     * Cancels a reservation (e.g. a shopper removed the item from their cart): the held stock is
     * returned to the available pool and the reservation is marked CANCELLED.
     *
     * @param reservationId the id of the reservation to cancel
     * @throws ReservationNotFoundException      if no reservation exists with this id
     * @throws InvalidReservationStateException if the reservation is not currently ACTIVE
     */
    public void cancelReservation(String reservationId) {
        Reservation reservation = requireActiveReservation(reservationId);
        InventoryItem item = getItem(reservation.getStoreId(), reservation.getProductId());
        item.release(reservation.getQuantity());
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    /**
     * Looks up a reservation by id and asserts it is still ACTIVE, since only active reservations
     * can be confirmed or cancelled.
     *
     * @param reservationId the id of the reservation to look up
     * @return the active reservation
     * @throws ReservationNotFoundException      if no reservation exists with this id
     * @throws InvalidReservationStateException if the reservation exists but is not ACTIVE
     */
    private Reservation requireActiveReservation(String reservationId) {
        Reservation reservation = reservations.get(reservationId);
        if (reservation == null) {
            throw new ReservationNotFoundException(reservationId);
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(reservationId, reservation.getStatus());
        }
        return reservation;
    }

    /**
     * Background sweep invoked periodically by the scheduler: finds every reservation that is
     * still marked ACTIVE but has passed its expiry instant, releases its stock back to the
     * available pool, and marks it EXPIRED. This is what returns stock from abandoned carts
     * without requiring an explicit cancel call.
     */
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

    /**
     * Returns the tracked inventory item for a (store, product) pair, primarily for display or
     * diagnostic purposes.
     *
     * @param storeId   the store id
     * @param productId the product id
     * @return the tracked inventory item
     * @throws ProductNotTrackedException if the (store, product) pair has never been stocked
     */
    public InventoryItem getInventoryItem(String storeId, String productId) {
        return getItem(storeId, productId);
    }

    /**
     * Updates the low-stock alert threshold for an already-tracked (store, product) pair (e.g. a
     * store admin adjusting merchandising rules for a product).
     *
     * @param storeId           the store id
     * @param productId         the product id
     * @param lowStockThreshold the new available-quantity threshold at/below which alerts fire
     * @throws ProductNotTrackedException if the (store, product) pair has never been stocked
     */
    public void updateLowStockThreshold(String storeId, String productId, int lowStockThreshold) {
        getItem(storeId, productId).setLowStockThreshold(lowStockThreshold);
    }

    /**
     * Stops the background expiry-sweep scheduler. Should be called on application shutdown so
     * the (non-daemon) scheduler thread does not keep the JVM alive.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
