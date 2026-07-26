package com.lowleveldesign.instacart.observer;

import com.lowleveldesign.instacart.inventory.InventoryItem;

/** Simple observer that logs/alerts when a product's stock drops at or below its threshold. */
public class LowStockNotifier implements InventoryObserver {
    @Override
    public void onLowStock(InventoryItem item) {
        System.out.printf("[ALERT] Low stock: %s has only %d units left at %s (threshold=%d)%n",
                item.getProduct().getName(), item.getAvailableQuantity(),
                item.getStore().getName(), item.getLowStockThreshold());
    }
}
