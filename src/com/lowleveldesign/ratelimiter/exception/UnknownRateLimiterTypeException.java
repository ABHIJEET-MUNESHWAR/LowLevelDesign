package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown by {@code RateLimiterFactory} when asked to build a {@code RateLimiterType} it does not
 * recognize. In practice this guards the {@code switch} default so that adding a new enum constant
 * without a matching factory branch fails loudly instead of silently returning null.
 */
public class UnknownRateLimiterTypeException extends RateLimiterException {

    public UnknownRateLimiterTypeException(String message) {
        super(message);
    }
}
