package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.exception.UnknownRateLimiterTypeException;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

/**
 * Factory + Strategy entry point: client code depends only on {@link RateLimiter} and picks a
 * concrete algorithm via {@link RateLimiterType} without knowing any implementation class names.
 * Swapping algorithms (e.g. token bucket in prod, sliding window log in a test asserting exact
 * behavior) is a one-line change at the call site.
 */
public final class RateLimiterFactory {

    /** Non-instantiable: this is a static utility holder. */
    private RateLimiterFactory() {
    }

    /**
     * Creates a rate limiter of the requested algorithm backed by the real system clock.
     *
     * @param type   which algorithm to instantiate
     * @param config the "permits per window" policy to enforce
     * @return a ready-to-use {@link RateLimiter}
     * @throws UnknownRateLimiterTypeException if {@code type} has no mapped implementation
     */
    public static RateLimiter create(RateLimiterType type, RateLimiterConfig config) {
        return create(type, config, Ticker.systemTicker());
    }

    /**
     * Creates a rate limiter of the requested algorithm with an explicit {@link Ticker},
     * primarily so tests can inject a deterministic fake clock.
     *
     * @param type   which algorithm to instantiate
     * @param config the "permits per window" policy to enforce
     * @param ticker the time source the limiter should read
     * @return a ready-to-use {@link RateLimiter}
     * @throws UnknownRateLimiterTypeException if {@code type} has no mapped implementation
     */
    public static RateLimiter create(RateLimiterType type, RateLimiterConfig config, Ticker ticker) {
        switch (type) {
            case TOKEN_BUCKET:
                return new TokenBucketRateLimiter(config, ticker);
            case FIXED_WINDOW_COUNTER:
                return new FixedWindowCounterRateLimiter(config, ticker);
            case SLIDING_WINDOW_LOG:
                return new SlidingWindowLogRateLimiter(config, ticker);
            case SLIDING_WINDOW_COUNTER:
                return new SlidingWindowCounterRateLimiter(config, ticker);
            default:
                throw new UnknownRateLimiterTypeException("Unknown rate limiter type: " + type);
        }
    }
}
