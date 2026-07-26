package com.lowleveldesign.ratelimiter.model;

import com.lowleveldesign.ratelimiter.exception.InvalidRateLimiterConfigException;

import java.time.Duration;

/**
 * Immutable policy for a rate limiter: "allow at most {@code permits} requests per
 * {@code window}". Every algorithm in this package (token bucket, fixed window, sliding
 * window log/counter) is configured with exactly this pair -- the differences between
 * algorithms are purely in *how* they enforce the same policy, not in what the policy says.
 *
 * <p>Token bucket additionally reads this as "capacity = permits, refill rate =
 * permits/window", which is what allows short bursts up to {@code permits} while the
 * long-run average settles at permits/window.
 */
public final class RateLimiterConfig {

    private final int permits;
    private final Duration window;

    private RateLimiterConfig(int permits, Duration window) {
        if (permits <= 0) {
            throw new InvalidRateLimiterConfigException("permits must be > 0, got " + permits);
        }
        if (window == null || window.isZero() || window.isNegative()) {
            throw new InvalidRateLimiterConfigException("window must be a positive duration, got " + window);
        }
        this.permits = permits;
        this.window = window;
    }

    public static RateLimiterConfig of(int permits, Duration window) {
        return new RateLimiterConfig(permits, window);
    }

    public int permits() {
        return permits;
    }

    public Duration window() {
        return window;
    }

    public long windowNanos() {
        return window.toNanos();
    }

    /** Nanoseconds a token-bucket implementation must wait, on average, to mint one token. */
    public double nanosPerPermit() {
        return (double) window.toNanos() / permits;
    }

    @Override
    public String toString() {
        return permits + " requests / " + window;
    }
}
