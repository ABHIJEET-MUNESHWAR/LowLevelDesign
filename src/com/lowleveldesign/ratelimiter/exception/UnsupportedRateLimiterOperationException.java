package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown when an algorithm is asked to perform an operation it cannot support -- for example,
 * requesting more than one permit at a time from {@code SlidingWindowLogRateLimiter}, whose exact
 * per-request timestamp log is defined only for single-permit acquisition.
 */
public class UnsupportedRateLimiterOperationException extends RateLimiterException {

    /**
     * Creates the exception describing the unsupported operation.
     *
     * @param message what was requested and why the algorithm cannot support it
     */
    public UnsupportedRateLimiterOperationException(String message) {
        super(message);
    }
}
