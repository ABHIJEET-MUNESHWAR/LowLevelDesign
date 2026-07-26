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

    public InventoryItem(Store store, Product product, int totalQuantity, int lowStockThreshold) {
        this.store = store;
        this.product = product;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.lowStockThreshold = lowStockThreshold;
    }

    public Store getStore() {
        return store;
    }

    public Product getProduct() {
        return product;
    }

    public int getAvailableQuantity() {
        lock.lock();
        try {
            return totalQuantity - reservedQuantity;
        } finally {
            lock.unlock();
        }
    }

    public int getTotalQuantity() {
        lock.lock();
        try {
            return totalQuantity;
        } finally {
            lock.unlock();
        }
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    /** Adds newly received stock (e.g. from a supplier restock). */
    public void addStock(int quantity) {
        lock.lock();
        try {
            totalQuantity += quantity;
        } finally {
            lock.unlock();
        }
    }

    /** Attempts to hold {@code quantity} units for a pending cart/order. Returns true on success. */
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

    /** Releases a previously held reservation without consuming stock (cancel / expiry). */
    public void release(int quantity) {
        lock.lock();
        try {
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
        } finally {
            lock.unlock();
        }
    }

    /** Confirms a reservation: permanently removes the stock (checkout / hand-off to shopper). */
    public void confirm(int quantity) {
        lock.lock();
        try {
            reservedQuantity = Math.max(0, reservedQuantity - quantity);
            totalQuantity = Math.max(0, totalQuantity - quantity);
        } finally {
            lock.unlock();
        }
    }

    public boolean isBelowThreshold() {
        return getAvailableQuantity() <= lowStockThreshold;
    }

    @Override
    public String toString() {
        return String.format("%s @ %s -> total=%d, reserved=%d, available=%d",
                product, store, getTotalQuantity(), reservedQuantity, getAvailableQuantity());
    }
}
