package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token bucket: each client owns a bucket holding at most {@code capacity} tokens, refilled
 * continuously at {@code capacity/window} tokens/nanosecond. A request consumes one token per
 * permit; it is allowed iff enough tokens are currently in the bucket.
 *
 * <p>Refill is computed lazily on access ("lazy refill") rather than via a background thread:
 * on every call we compute how much time elapsed since the bucket was last touched, top up the
 * bucket by that much (capped at capacity), then try to spend. This gives exact, jitter-free
 * behavior with zero background threads and O(1) work per call.
 *
 * <p>Bursty but bounded: a client that has been idle can spend up to {@code capacity} permits
 * instantly, then must wait for the refill rate to replenish -- unlike a fixed window, there is
 * no hard reset edge that lets two bursts land back-to-back.
 */
public final class TokenBucketRateLimiter implements RateLimiter {

    /** Per-client mutable state; also serves as the client's own monitor lock. */
    private static final class Bucket {
        double tokens;
        long lastRefillNanos;
    }

    private final RateLimiterConfig config;
    private final Ticker ticker;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(RateLimiterConfig config, Ticker ticker) {
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
        Bucket bucket = buckets.computeIfAbsent(clientId, id -> {
            Bucket b = new Bucket();
            b.tokens = config.permits();
            b.lastRefillNanos = ticker.nanoTime();
            return b;
        });

        synchronized (bucket) {
            refill(bucket);
            if (bucket.tokens >= permits) {
                bucket.tokens -= permits;
                return RateLimitResult.allow((long) bucket.tokens);
            }
            double missing = permits - bucket.tokens;
            long retryAfterNanos = (long) (missing * config.nanosPerPermit());
            return RateLimitResult.deny(retryAfterNanos / 1_000_000L);
        }
    }

    private void refill(Bucket bucket) {
        long now = ticker.nanoTime();
        long elapsed = now - bucket.lastRefillNanos;
        if (elapsed <= 0) {
            return;
        }
        double refilled = elapsed / config.nanosPerPermit();
        bucket.tokens = Math.min(config.permits(), bucket.tokens + refilled);
        bucket.lastRefillNanos = now;
    }
}
