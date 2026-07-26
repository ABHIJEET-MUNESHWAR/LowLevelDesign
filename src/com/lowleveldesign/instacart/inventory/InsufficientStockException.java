package com.lowleveldesign.instacart.inventory;

/** Thrown when a reservation cannot be satisfied by current available stock. */
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId, String storeId, int requested, int available) {
        super(String.format("Cannot reserve %d of product %s at store %s (only %d available)",
                requested, productId, storeId, available));
    }
}
