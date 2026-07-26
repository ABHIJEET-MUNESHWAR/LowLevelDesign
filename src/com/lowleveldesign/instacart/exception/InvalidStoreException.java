package com.lowleveldesign.instacart.exception;

/** Thrown when a Store is constructed with invalid data (blank id or name). */
public class InvalidStoreException extends InstacartException {
    /**
     * Creates the exception for an invalid store construction attempt.
     *
     * @param message the failure description
     */
    public InvalidStoreException(String message) {
        super(message);
    }
}
