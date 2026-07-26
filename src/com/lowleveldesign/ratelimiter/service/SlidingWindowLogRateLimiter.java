package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.exception.InvalidRateLimitRequestException;
import com.lowleveldesign.ratelimiter.exception.UnsupportedRateLimiterOperationException;
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

    /**
     * Constructs a sliding-window-log limiter.
     *
     * @param config the policy: at most {@code permits} requests in any trailing {@code window}
     * @param ticker the time source used to timestamp and age out requests
     */
    public SlidingWindowLogRateLimiter(RateLimiterConfig config, Ticker ticker) {
        this.config = config;
        this.ticker = ticker;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #tryAcquireDetailed(String, int)} and returns only the boolean verdict.
     */
    @Override
    public boolean tryAcquire(String clientId, int permits) {
        return tryAcquireDetailed(clientId, permits).allowed();
    }

    /**
     * Evicts timestamps older than {@code now - window} from the client's log, then admits the
     * request iff fewer than {@code permits} entries remain, appending {@code now} on success. All
     * of this runs under the log's monitor lock so it is atomic per client.
     *
     * @param clientId identifier of the tenant being limited
     * @param permits  must be exactly {@code 1}; this algorithm's exact log is defined only for
     *                 single-permit acquisition
     * @return an allow result with the remaining count, or a deny result whose retry-after is when
     *         the oldest logged request will exit the window
     * @throws InvalidRateLimitRequestException           if {@code permits <= 0}
     * @throws UnsupportedRateLimiterOperationException    if {@code permits != 1}
     */
    @Override
    public RateLimitResult tryAcquireDetailed(String clientId, int permits) {
        if (permits <= 0) {
            throw new InvalidRateLimitRequestException("permits must be > 0, got " + permits);
        }
        // Only single-permit requests make sense for an exact per-request log.
        if (permits != 1) {
            throw new UnsupportedRateLimiterOperationException(
                    "SlidingWindowLogRateLimiter only supports acquiring 1 permit at a time, got " + permits);
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
