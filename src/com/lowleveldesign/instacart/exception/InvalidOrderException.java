package com.lowleveldesign.instacart.exception;

/** Thrown when an order is placed with no line items (null or empty list). */
public class InvalidOrderException extends InstacartException {
    /**
     * Creates the exception for a malformed order request.
     *
     * @param message the failure description
     */
    public InvalidOrderException(String message) {
        super(message);
    }
}
