package com.lowleveldesign.ratelimiter.service;

/** The rate-limiting algorithms this package implements; see {@link RateLimiterFactory}. */
public enum RateLimiterType {
    TOKEN_BUCKET,
    FIXED_WINDOW_COUNTER,
    SLIDING_WINDOW_LOG,
    SLIDING_WINDOW_COUNTER
}
