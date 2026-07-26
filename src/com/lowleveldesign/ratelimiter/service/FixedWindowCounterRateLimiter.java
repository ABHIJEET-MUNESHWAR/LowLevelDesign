package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.exception.InvalidRateLimitRequestException;
import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed window counter: time is sliced into consecutive, non-overlapping windows of
 * {@code config.window()} (aligned to epoch, so all clients share the same window boundaries).
 * Each client gets a counter that resets to zero whenever the current window changes; a request
 * is allowed iff the counter for the active window is below {@code permits}.
 *
 * <p>Simplest and cheapest algorithm (O(1) time and space per client), but it has the classic
 * "boundary burst" problem: a client can send {@code permits} requests in the last instant of one
 * window and another {@code permits} in the first instant of the next, i.e. {@code 2x permits} in
 * a short span straddling the boundary. {@link SlidingWindowCounterRateLimiter} fixes this at
 * modest extra cost.
 */
public final class FixedWindowCounterRateLimiter implements RateLimiter {

    private static final class WindowState {
        long windowIndex = -1;
        int count;
    }

    private final RateLimiterConfig config;
    private final Ticker ticker;
    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>();

    /**
     * Constructs a fixed-window-counter limiter.
     *
     * @param config the policy: at most {@code permits} requests per fixed {@code window}
     * @param ticker the time source used to derive the current window index
     */
    public FixedWindowCounterRateLimiter(RateLimiterConfig config, Ticker ticker) {
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
     * Resets the client's counter if the epoch-aligned window has rolled over, then admits the
     * request iff the counter for the current window stays within {@code permits}. The
     * check-and-increment runs under the state's monitor lock so it is atomic per client.
     *
     * @param clientId identifier of the tenant being limited
     * @param permits  number of permits to consume; must be positive
     * @return an allow result with the remaining count, or a deny result whose retry-after is the
     *         time until the current window ends
     * @throws InvalidRateLimitRequestException if {@code permits <= 0}
     */
    @Override
    public RateLimitResult tryAcquireDetailed(String clientId, int permits) {
        if (permits <= 0) {
            throw new InvalidRateLimitRequestException("permits must be > 0, got " + permits);
        }
        WindowState state = states.computeIfAbsent(clientId, id -> new WindowState());
        long windowNanos = config.windowNanos();
        long now = ticker.nanoTime();
        long currentWindow = Math.floorDiv(now, windowNanos);

        synchronized (state) {
            if (state.windowIndex != currentWindow) {
                state.windowIndex = currentWindow;
                state.count = 0;
            }
            if (state.count + permits <= config.permits()) {
                state.count += permits;
                return RateLimitResult.allow(config.permits() - state.count);
            }
            long windowEndNanos = (currentWindow + 1) * windowNanos;
            return RateLimitResult.deny((windowEndNanos - now) / 1_000_000L);
        }
    }
}
