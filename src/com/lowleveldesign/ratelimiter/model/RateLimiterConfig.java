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

    /**
     * Validates and stores the policy. Private so instances can only be obtained via
     * {@link #of(int, Duration)}, guaranteeing every {@code RateLimiterConfig} is valid.
     *
     * @param permits the maximum permits per window; must be positive
     * @param window  the window duration; must be positive
     * @throws InvalidRateLimiterConfigException if either argument is invalid
     */
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

    /**
     * Creates a validated policy.
     *
     * @param permits the maximum number of permits allowed per {@code window}; must be positive
     * @param window  the length of the rolling/reset window; must be a positive duration
     * @return an immutable {@code RateLimiterConfig}
     * @throws InvalidRateLimiterConfigException if {@code permits <= 0} or {@code window} is
     *                                           null, zero, or negative
     */
    public static RateLimiterConfig of(int permits, Duration window) {
        return new RateLimiterConfig(permits, window);
    }

    /**
     * Returns the maximum number of permits allowed per window (the policy's numerator).
     *
     * @return the configured permit count, always positive
     */
    public int permits() {
        return permits;
    }

    /**
     * Returns the window duration over which {@link #permits()} applies (the policy's denominator).
     *
     * @return the configured window duration, always positive
     */
    public Duration window() {
        return window;
    }

    /**
     * Returns the window length in nanoseconds, the unit the algorithms compare against
     * {@code Ticker.nanoTime()}.
     *
     * @return the window duration converted to nanoseconds
     */
    public long windowNanos() {
        return window.toNanos();
    }

    /**
     * Returns how many nanoseconds must elapse, on average, to accrue a single permit --
     * i.e. {@code windowNanos / permits}. Token bucket uses this both to compute lazy refill
     * amounts and to estimate a retry-after hint when denied.
     *
     * @return average nanoseconds required to mint one permit
     */
    public double nanosPerPermit() {
        return (double) window.toNanos() / permits;
    }

    /**
     * Returns a human-readable summary of this policy, e.g. {@code "5 requests / PT1S"}.
     *
     * @return a display string of the form {@code "<permits> requests / <window>"}
     */
    @Override
    public String toString() {
        return permits + " requests / " + window;
    }
}
