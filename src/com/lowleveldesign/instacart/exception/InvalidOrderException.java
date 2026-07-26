package com.lowleveldesign.instacart.exception;

/** Thrown when an order is placed with no line items (null or empty list). */
public class InvalidOrderException extends InstacartException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
