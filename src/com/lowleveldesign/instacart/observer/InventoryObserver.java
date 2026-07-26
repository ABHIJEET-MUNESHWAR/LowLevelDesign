package com.lowleveldesign.instacart.observer;

import com.lowleveldesign.instacart.inventory.InventoryItem;

/** Observer interface for reacting to inventory events (Observer pattern). */
public interface InventoryObserver {
    /**
     * Called by {@link com.lowleveldesign.instacart.inventory.InventoryManager} whenever a stock
     * change causes an item's available quantity to drop at or below its configured low-stock
     * threshold.
     *
     * @param item the inventory item that is now at/below its low-stock threshold
     */
    void onLowStock(InventoryItem item);
}
