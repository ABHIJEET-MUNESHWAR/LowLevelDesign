package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown when a {@code RateLimiterConfig} is constructed with an invalid policy: a non-positive
 * permit count, or a window that is null, zero, or negative. Raised at construction time so an
 * impossible policy can never reach an algorithm.
 */
public class InvalidRateLimiterConfigException extends RateLimiterException {

    public InvalidRateLimiterConfigException(String message) {
        super(message);
    }
}
