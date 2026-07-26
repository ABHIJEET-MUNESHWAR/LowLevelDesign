package com.lowleveldesign.instacart.inventory;

import com.lowleveldesign.instacart.model.Product;
import com.lowleveldesign.instacart.model.Store;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks stock for a single (Store, Product) pair.
 *
 * Quantity bookkeeping:
 *   totalQuantity    -> physically on the shelf
 *   reservedQuantity -> held by active (not yet confirmed) shopper carts
 *   availableQuantity = totalQuantity - reservedQuantity
 *
 * A per-item lock keeps reserve/confirm/release/restock atomic without
 * locking the whole inventory map, so unrelated products/stores don't
 * contend with each other.
 */
public class InventoryItem {
    private final Store store;
    private final Product product;
    private int totalQuantity;
    private int reservedQuantity;
    private int lowStockThreshold;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Creates a new stock record for a (store, product) pair with no active reservations.
     *
     * @param store             the store this stock belongs to
     * @param product           the product being tracked
     * @param totalQuantity     initial physical quantity on the shelf
     * @param lowStockThreshold the available-quantity level at/below which this item is considered low stock
     */
    public InventoryItem(Store store, Product product, int totalQuantity, int lowStockThreshold) {
        this.store = store;
        this.product = product;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * Returns the store this stock record belongs to.
     *
     * @return the store
     */
    public Store getStore() {
        return store;
    }

    /**
     * Returns the product this stock record tracks.
     *
     * @return the product
     */
    public Product getProduct() {
        return product;
    }

    /**
     * Computes the quantity currently free to reserve, i.e. total stock minus whatever is
     * already held by active reservations.
     *
     * @return the available quantity ({@code totalQuantity - reservedQuantity})
     */
    public int getAvailableQuantity() {
        lock.lock();
        try {
            return totalQuantity - reservedQuantity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the physical quantity on the shelf, including units held by active reservations.
     *
     * @return the total quantity
     */
    public int getTotalQuantity() {
        lock.lock();
        try {
            return totalQuantity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the available-quantity level at/below which this item is flagged as low stock.
     *
     * @return the low-stock threshold
     */
    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    /**
     * Updates the low-stock threshold, e.g. when merchandising rules change for this product.
     *
     * @param lowStockThreshold the new threshold
     */
    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    /**
     * Adds newly received stock (e.g. from a supplier restock).
     *
     * @param quantity the quantity to add to the total on-shelf count
     */
    public void addStock(int quantity) {
        lock.lock();
        try {
            totalQuantity += quantity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Attempts to hold {@code quantity} units for a pending cart/order. The check-then-reserve
     * sequence is performed under the item's lock so concurrent callers can never oversell the
     * same stock.
     *
     * @param quantity the quantity to hold
     * @return true if enough stock was available and the reservation was recorded; false if there
     *         was insufficient available stock (in which case nothing is changed)
     */
    public boolean reserve(int quantity) {
        lock.lock();
        try {
            if (totalQuantity - reservedQuantity < quantity) {
                return false;
            }
            reservedQuantity += quantity;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases a previously held reservation without consuming stock (used on cancellation or
     * automatic expiry), returning the quantity to the available pool.
     *
     * @param quantity the quantity to release back to the available pool
     */
    public void release(int quantity) {
        lock.lock();
        try {
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Confirms a reservation: permanently removes the stock from both the reserved and total
     * counts (checkout / hand-off to shopper).
     *
     * @param quantity the quantity to permanently deduct
     */
    public void confirm(int quantity) {
        lock.lock();
        try {
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
            totalQuantity = Math.max(0, totalQuantity - quantity);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks whether the currently available quantity has dropped at or below the configured
     * low-stock threshold.
     *
     * @return true if {@code getAvailableQuantity() <= getLowStockThreshold()}
     */
    public boolean isBelowThreshold() {
        return getAvailableQuantity() <= lowStockThreshold;
    }

    /**
     * Returns a human-readable summary of this item's product, store, and current quantities.
     *
     * @return a diagnostic string suitable for logging/demo output
     */
    @Override
    public String toString() {
        return String.format("%s @ %s -> total=%d, reserved=%d, available=%d",
                product, store, getTotalQuantity(), reservedQuantity, getAvailableQuantity());
    }
}
