package com.lowleveldesign.splitwise.exception;

/**
 * Base type for all domain-specific exceptions thrown by the Splitwise
 * application. Being unchecked keeps the fluent API clean while still
 * allowing callers to catch {@code SplitwiseException} to handle every
 * business-rule violation in one place.
 */
public class SplitwiseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with a detail message.
     *
     * @param message description of the violation
     */
    public SplitwiseException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a detail message and underlying cause.
     *
     * @param message description of the violation
     * @param cause   the underlying cause
     */
    public SplitwiseException(String message, Throwable cause) {
        super(message, cause);
    }
}
