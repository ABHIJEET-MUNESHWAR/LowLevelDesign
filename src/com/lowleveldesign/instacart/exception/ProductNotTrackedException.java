package com.lowleveldesign.instacart.exception;

/** Thrown when a lookup is attempted for a (store, product) pair that was never stocked. */
public class ProductNotTrackedException extends InstacartException {
    public ProductNotTrackedException(String productId, String storeId) {
        super("Product " + productId + " is not tracked at store " + storeId);
    }
}
