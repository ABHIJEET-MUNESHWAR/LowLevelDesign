package com.lowleveldesign.splitwise.exception;

/**
 * Base type for all domain-specific exceptions thrown by the Splitwise
 * application. Being unchecked keeps the fluent API clean while still
 * allowing callers to catch {@code SplitwiseException} to handle every
 * business-rule violation in one place.
 */
public class SplitwiseException extends RuntimeException {

    public SplitwiseException(String message) {
        super(message);
    }

    public SplitwiseException(String message, Throwable cause) {
        super(message, cause);
    }
}
