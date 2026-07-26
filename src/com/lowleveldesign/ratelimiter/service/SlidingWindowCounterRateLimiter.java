package com.lowleveldesign.ratelimiter.service;

import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window counter: an O(1)-memory approximation of the sliding window log. Like
 * {@link FixedWindowCounterRateLimiter}, time is sliced into fixed windows with a counter each,
 * but a request's admission is decided using a weighted blend of the *previous* window's count
 * and the *current* window's count:
 *
 * <pre>
 *   estimated = previousWindowCount * overlapFraction + currentWindowCount
 *   overlapFraction = 1 - (elapsedInCurrentWindow / windowSize)
 * </pre>
 *
 * <p>Intuition: {@code overlapFraction} is how much of the trailing {@code window} still falls
 * inside the previous bucket. Right at a window boundary it's ~1 (almost the whole previous
 * window still counts), and it decays linearly to 0 by the end of the current window. This
 * removes the fixed-window's boundary-burst problem (a full previous-window burst is still
 * counted, tapering smoothly) while keeping O(1) memory/time per client -- no per-request log
 * required, unlike {@link SlidingWindowLogRateLimiter}. The trade-off is that it assumes requests
 * are evenly spread within the previous window, which is an approximation, not exact.
 */
public final class SlidingWindowCounterRateLimiter implements RateLimiter {

    private static final class WindowState {
        long currentWindowIndex = -1;
        int currentCount;
        int previousCount;
    }

    private final RateLimiterConfig config;
    private final Ticker ticker;
    private final ConcurrentHashMap<String, WindowState> states = new ConcurrentHashMap<>();

    public SlidingWindowCounterRateLimiter(RateLimiterConfig config, Ticker ticker) {
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
        WindowState state = states.computeIfAbsent(clientId, id -> new WindowState());
        long windowNanos = config.windowNanos();
        long now = ticker.nanoTime();
        long currentWindow = Math.floorDiv(now, windowNanos);

        synchronized (state) {
            advanceWindow(state, currentWindow);

            long elapsedInCurrentWindow = now - currentWindow * windowNanos;
            double overlapFraction = 1.0 - ((double) elapsedInCurrentWindow / windowNanos);
            double estimated = state.previousCount * overlapFraction + state.currentCount;

            if (estimated + permits <= config.permits()) {
                state.currentCount += permits;
                return RateLimitResult.allow((long) (config.permits() - estimated - permits));
            }
            long windowEndNanos = (currentWindow + 1) * windowNanos;
            return RateLimitResult.deny((windowEndNanos - now) / 1_000_000L);
        }
    }

    /** Rolls {@code currentCount}/{@code previousCount} forward, handling gaps of >= 2 windows. */
    private void advanceWindow(WindowState state, long currentWindow) {
        if (state.currentWindowIndex == currentWindow) {
            return;
        }
        if (state.currentWindowIndex == currentWindow - 1) {
            state.previousCount = state.currentCount;
        } else {
            // Client has been idle for 2+ windows: no overlap with any prior traffic.
            state.previousCount = 0;
        }
        state.currentCount = 0;
        state.currentWindowIndex = currentWindow;
    }
}
