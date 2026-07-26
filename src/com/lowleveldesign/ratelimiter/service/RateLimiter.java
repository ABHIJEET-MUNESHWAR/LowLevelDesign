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
     * Attempts to consume a single permit for {@code clientId}. Convenience form of
     * {@link #tryAcquire(String, int)} with {@code permits == 1}.
     *
     * @param clientId identifier of the tenant being limited (API key, user id, IP, ...)
     * @return {@code true} if the request is allowed, {@code false} if it should be rejected
     *         (e.g. with an HTTP 429)
     */
    default boolean tryAcquire(String clientId) {
        return tryAcquire(clientId, 1);
    }

    /**
     * Attempts to consume {@code permits} permits for {@code clientId} atomically
     * (all-or-nothing): either every permit is granted or none is.
     *
     * @param clientId identifier of the tenant being limited
     * @param permits  number of permits to consume; must be positive
     * @return {@code true} if the permits were granted, {@code false} if the request is throttled
     * @throws com.lowleveldesign.ratelimiter.exception.InvalidRateLimitRequestException if
     *         {@code permits <= 0}
     */
    boolean tryAcquire(String clientId, int permits);

    /**
     * Same decision as {@link #tryAcquire(String, int)} but returns a richer
     * {@link RateLimitResult} (remaining permits / retry-after hint) suitable for populating
     * response headers.
     *
     * @param clientId identifier of the tenant being limited
     * @param permits  number of permits to consume; must be positive
     * @return a {@link RateLimitResult} describing the outcome
     * @throws com.lowleveldesign.ratelimiter.exception.InvalidRateLimitRequestException if
     *         {@code permits <= 0}
     */
    RateLimitResult tryAcquireDetailed(String clientId, int permits);

    /**
     * Detailed single-permit decision. Convenience form of
     * {@link #tryAcquireDetailed(String, int)} with {@code permits == 1}.
     *
     * @param clientId identifier of the tenant being limited
     * @return a {@link RateLimitResult} describing the outcome
     */
    default RateLimitResult tryAcquireDetailed(String clientId) {
        return tryAcquireDetailed(clientId, 1);
    }
}
