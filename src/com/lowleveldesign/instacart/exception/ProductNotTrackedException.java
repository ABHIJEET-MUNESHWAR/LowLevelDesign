package com.lowleveldesign.instacart.exception;

/** Thrown when a lookup is attempted for a (store, product) pair that was never stocked. */
public class ProductNotTrackedException extends InstacartException {
    /**
     * Creates the exception for a lookup of an untracked (store, product) pair.
     *
     * @param productId the product id that was looked up
     * @param storeId   the store id that was looked up
     */
    public ProductNotTrackedException(String productId, String storeId) {
        super("Product " + productId + " is not tracked at store " + storeId);
    }
}
