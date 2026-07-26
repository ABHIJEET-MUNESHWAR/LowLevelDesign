package com.lowleveldesign.instacart.exception;

/** Thrown when a reservation request exceeds the currently available stock for a product at a store. */
public class InsufficientStockException extends InstacartException {
    public InsufficientStockException(String productId, String storeId, int requested, int available) {
        super(String.format("Cannot reserve %d of product %s at store %s (only %d available)",
                requested, productId, storeId, available));
    }
}
