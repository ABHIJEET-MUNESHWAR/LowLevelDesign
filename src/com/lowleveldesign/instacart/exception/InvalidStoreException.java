package com.lowleveldesign.instacart.exception;

/** Thrown when a Store is constructed with invalid data (blank id or name). */
public class InvalidStoreException extends InstacartException {
    public InvalidStoreException(String message) {
        super(message);
    }
}
