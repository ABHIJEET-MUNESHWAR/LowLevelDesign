package com.lowleveldesign.ratelimiter.exception;

/**
 * Thrown when a call to {@code tryAcquire}/{@code tryAcquireDetailed} supplies an invalid
 * argument -- most commonly a non-positive {@code permits} count. This is a caller mistake, not a
 * rate-limiting outcome: being throttled is reported via {@code RateLimitResult#allowed()}
 * returning {@code false}, never via an exception.
 */
public class InvalidRateLimitRequestException extends RateLimiterException {

    public InvalidRateLimitRequestException(String message) {
        super(message);
    }
}
