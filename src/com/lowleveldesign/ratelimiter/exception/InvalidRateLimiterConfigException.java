package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown when a {@code RateLimiterConfig} is constructed with an invalid policy: a non-positive
 * permit count, or a window that is null, zero, or negative. Raised at construction time so an
 * impossible policy can never reach an algorithm.
 */
public class InvalidRateLimiterConfigException extends RateLimiterException {

    /**
     * Creates the exception describing the invalid policy.
     *
     * @param message the specific validation that failed (e.g. non-positive permits or window)
     */
    public InvalidRateLimiterConfigException(String message) {
        super(message);
    }
}
