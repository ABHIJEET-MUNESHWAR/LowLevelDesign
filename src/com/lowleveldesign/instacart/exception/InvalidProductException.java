package com.lowleveldesign.instacart.exception;

/** Thrown when a Product is constructed with invalid data (blank id/name or negative price). */
public class InvalidProductException extends InstacartException {
    /**
     * Creates the exception for an invalid product construction attempt.
     *
     * @param message the failure description
     */
    public InvalidProductException(String message) {
        super(message);
    }
}
