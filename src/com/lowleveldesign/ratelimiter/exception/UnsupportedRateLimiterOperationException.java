package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown when an algorithm is asked to perform an operation it cannot support -- for example,
 * requesting more than one permit at a time from {@code SlidingWindowLogRateLimiter}, whose exact
 * per-request timestamp log is defined only for single-permit acquisition.
 */
public class UnsupportedRateLimiterOperationException extends RateLimiterException {

    public UnsupportedRateLimiterOperationException(String message) {
        super(message);
    }
}
