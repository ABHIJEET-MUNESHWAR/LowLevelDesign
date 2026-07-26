package com.lowleveldesign.instacart.exception;

/** Thrown when a reservation request exceeds the currently available stock for a product at a store. */
public class InsufficientStockException extends InstacartException {
    /**
     * Creates the exception for a reservation request that exceeds available stock.
     *
     * @param productId the product that was requested
     * @param storeId   the store the product was requested from
     * @param requested the quantity that was requested
     * @param available the quantity that was actually available
     */
    public InsufficientStockException(String productId, String storeId, int requested, int available) {
        super(String.format("Cannot reserve %d of product %s at store %s (only %d available)",
                requested, productId, storeId, available));
    }
}
