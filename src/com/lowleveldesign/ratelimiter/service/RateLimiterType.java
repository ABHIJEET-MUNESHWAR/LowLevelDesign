package com.lowleveldesign.ratelimiter.service;

/** The rate-limiting algorithms this package implements; see {@link RateLimiterFactory}. */
public enum RateLimiterType {
    /** Lazy-refill token bucket: allows bounded bursts up to the permit count, then steady rate. */
    TOKEN_BUCKET,
    /** Epoch-aligned per-window counter: cheapest, but allows up to 2x permits across a boundary. */
    FIXED_WINDOW_COUNTER,
    /** Exact per-request timestamp log: no boundary burst, O(permits) memory per client. */
    SLIDING_WINDOW_LOG,
    /** Weighted previous+current window approximation: O(1) memory, smooths the boundary burst. */
    SLIDING_WINDOW_COUNTER
}
