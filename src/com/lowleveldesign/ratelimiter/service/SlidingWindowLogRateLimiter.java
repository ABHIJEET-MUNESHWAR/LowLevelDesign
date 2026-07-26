package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window log: each client keeps an exact log (timestamps, nanoseconds) of every request
 * accepted in the last {@code config.window()}. On a new request, timestamps older than
 * {@code now - window} are evicted from the front of the log, then the request is allowed iff
 * the remaining log size is below {@code permits}, in which case its timestamp is appended.
 *
 * <p>This is the "ground truth" algorithm -- perfectly accurate, no boundary burst, no
 * approximation -- at the cost of O(permits) memory per client and O(permits) eviction work per
 * call (amortized O(1) per entry since each timestamp is pushed and popped exactly once).
 * {@link SlidingWindowCounterRateLimiter} trades a little accuracy for O(1) memory when
 * {@code permits} is large.
 */
public final class SlidingWindowLogRateLimiter implements RateLimiter {

    private final RateLimiterConfig config;
    private final Ticker ticker;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> logs = new ConcurrentHashMap<>();

    public SlidingWindowLogRateLimiter(RateLimiterConfig config, Ticker ticker) {
        this.config = config;
        this.ticker = ticker;
    }

    @Override
    public boolean tryAcquire(String clientId, int permits) {
        return tryAcquireDetailed(clientId, permits).allowed();
    }

    @Override
    public RateLimitResult tryAcquireDetailed(String clientId, int permits) {
        if (permits <= 0) {
            throw new IllegalArgumentException("permits must be > 0");
        }
        // Only single-permit requests make sense for an exact per-request log.
        if (permits != 1) {
            throw new UnsupportedOperationException(
                    "SlidingWindowLogRateLimiter only supports acquiring 1 permit at a time");
        }
        ArrayDeque<Long> log = logs.computeIfAbsent(clientId, id -> new ArrayDeque<>());
        long now = ticker.nanoTime();
        long windowStart = now - config.windowNanos();

        synchronized (log) {
            while (!log.isEmpty() && log.peekFirst() <= windowStart) {
                log.pollFirst();
            }
            if (log.size() < config.permits()) {
                log.addLast(now);
                return RateLimitResult.allow(config.permits() - log.size());
            }
            long oldestNanos = log.peekFirst();
            long retryAfterNanos = (oldestNanos + config.windowNanos()) - now;
            return RateLimitResult.deny(retryAfterNanos / 1_000_000L);
        }
    }
}
