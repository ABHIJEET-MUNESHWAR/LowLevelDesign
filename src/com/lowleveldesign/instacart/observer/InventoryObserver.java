package com.lowleveldesign.instacart.observer;

import com.lowleveldesign.instacart.inventory.InventoryItem;

/** Observer interface for reacting to inventory events (Observer pattern). */
public interface InventoryObserver {
    void onLowStock(InventoryItem item);
}
