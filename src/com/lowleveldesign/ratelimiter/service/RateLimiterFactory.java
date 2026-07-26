package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

/**
 * Factory + Strategy entry point: client code depends only on {@link RateLimiter} and picks a
 * concrete algorithm via {@link RateLimiterType} without knowing any implementation class names.
 * Swapping algorithms (e.g. token bucket in prod, sliding window log in a test asserting exact
 * behavior) is a one-line change at the call site.
 */
public final class RateLimiterFactory {

    private RateLimiterFactory() {
    }

    public static RateLimiter create(RateLimiterType type, RateLimiterConfig config) {
        return create(type, config, Ticker.systemTicker());
    }

    /** Overload that accepts a {@link Ticker}, primarily so tests can inject a fake clock. */
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
                throw new IllegalArgumentException("Unknown rate limiter type: " + type);
        }
    }
}
