package com.lowleveldesign.instacart.exception;

/** Thrown when a Product is constructed with invalid data (blank id/name or negative price). */
public class InvalidProductException extends InstacartException {
    public InvalidProductException(String message) {
        super(message);
    }
}
