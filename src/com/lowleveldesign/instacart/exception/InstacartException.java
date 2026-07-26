package com.lowleveldesign.instacart.exception;

/**
 * Root of the custom exception hierarchy for this system. Every domain-specific failure
 * (invalid input, missing entity, illegal state transition, business rule violation) extends
 * this class instead of relying on generic JDK exceptions, so callers can distinguish "expected"
 * Instacart domain failures from programming errors and, if desired, catch them all at once via
 * this common base.
 */
public abstract class InstacartException extends RuntimeException {
    /**
     * Creates the exception with a human-readable description of the domain failure.
     *
     * @param message the failure description
     */
    protected InstacartException(String message) {
        super(message);
    }
}
