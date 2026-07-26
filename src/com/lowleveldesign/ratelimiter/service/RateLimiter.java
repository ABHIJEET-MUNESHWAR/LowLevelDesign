package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.model.RateLimitResult;

/**
 * Public contract for every rate limiter in this package. A {@code RateLimiter} is
 * multi-tenant: it tracks independent state per {@code clientId} (e.g. API key, user id, IP)
 * behind a single instance, so one limiter can gate an entire service rather than needing one
 * object per client wired up by hand.
 *
 * <p>Implementations must be safe for concurrent use by multiple threads deciding for the same
 * or different clients at the same time.
 */
public interface RateLimiter {

    /**
     * Attempts to consume one permit for {@code clientId}.
     *
     * @return {@code true} if the request is allowed, {@code false} if it should be rejected
     *         (e.g. with an HTTP 429)
     */
    default boolean tryAcquire(String clientId) {
        return tryAcquire(clientId, 1);
    }

    /** Attempts to consume {@code permits} permits for {@code clientId} atomically (all-or-nothing). */
    boolean tryAcquire(String clientId, int permits);

    /**
     * Same decision as {@link #tryAcquire(String, int)} but returns a richer
     * {@link RateLimitResult} (remaining permits / retry-after hint) suitable for populating
     * response headers.
     */
    RateLimitResult tryAcquireDetailed(String clientId, int permits);

    default RateLimitResult tryAcquireDetailed(String clientId) {
        return tryAcquireDetailed(clientId, 1);
    }
}
