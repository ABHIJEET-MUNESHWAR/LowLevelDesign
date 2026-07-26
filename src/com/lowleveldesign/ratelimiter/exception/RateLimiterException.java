package com.lowleveldesign.ratelimiter.exception;

/**
 * Base type for every exception raised by this rate limiter. Unchecked (extends
 * {@link RuntimeException}) because these all signal programming/configuration mistakes -- an
 * invalid policy, a bad {@code permits} argument, an unknown algorithm type -- that a caller
 * should fix at the call site, not routinely catch and recover from. A single common supertype
 * lets a caller who does want to handle them {@code catch (RateLimiterException e)} once instead
 * of enumerating every subtype.
 */
public abstract class RateLimiterException extends RuntimeException {

    /**
     * Creates the exception with a human-readable explanation.
     *
     * @param message describes what was invalid or unsupported
     */
    protected RateLimiterException(String message) {
        super(message);
    }
}
