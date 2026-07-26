package com.lowleveldesign.ratelimiter.test;

import com.lowleveldesign.ratelimiter.exception.InvalidRateLimitRequestException;
import com.lowleveldesign.ratelimiter.exception.InvalidRateLimiterConfigException;
import com.lowleveldesign.ratelimiter.exception.UnsupportedRateLimiterOperationException;
import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.model.Ticker;
import com.lowleveldesign.ratelimiter.service.RateLimiter;
import com.lowleveldesign.ratelimiter.service.RateLimiterFactory;
import com.lowleveldesign.ratelimiter.service.RateLimiterType;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Dependency-free (no JUnit) correctness and concurrency test suite. Uses a manually-advanced
 * {@link FakeTicker} instead of {@code Thread.sleep} so window/refill boundaries can be asserted
 * exactly and deterministically.
 *
 * <p>Run with: {@code java -cp out com.lowleveldesign.ratelimiter.test.TestRunner}
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Runs the whole suite and exits non-zero if any assertion failed.
     *
     * @param args ignored
     * @throws InterruptedException if the concurrency test's thread coordination is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        testTokenBucketBurstThenThrottleThenRefill();
        testFixedWindowResetsOnBoundary();
        testFixedWindowBoundaryBurstIsAllowed();
        testSlidingWindowLogExactAccounting();
        testSlidingWindowLogRetryAfterMatchesOldestEntry();
        testSlidingWindowCounterSmoothsBoundaryBurst();
        testPerClientIsolation();
        testCustomExceptionsAreThrown();
        testConcurrentRaceGrantsExactlyCapacityPermits();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Verifies the token bucket allows a full burst, denies once empty, then refills over time. */
    private static void testTokenBucketBurstThenThrottleThenRefill() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(3, Duration.ofSeconds(3)); // 1 token/sec
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config, ticker);

        assertTrue("burst permit 1", limiter.tryAcquire("c1"));
        assertTrue("burst permit 2", limiter.tryAcquire("c1"));
        assertTrue("burst permit 3", limiter.tryAcquire("c1"));
        assertFalse("4th request exhausts bucket", limiter.tryAcquire("c1"));

        ticker.advance(Duration.ofMillis(999));
        assertFalse("just under 1s: still no full token", limiter.tryAcquire("c1"));

        ticker.advance(Duration.ofMillis(2));
        assertTrue("just over 1s: exactly one token refilled", limiter.tryAcquire("c1"));
        assertFalse("second token not yet available", limiter.tryAcquire("c1"));
    }

    /** Verifies the fixed-window counter resets to zero when the window rolls over. */
    private static void testFixedWindowResetsOnBoundary() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.FIXED_WINDOW_COUNTER, config, ticker);

        assertTrue("permit 1", limiter.tryAcquire("c2"));
        assertTrue("permit 2", limiter.tryAcquire("c2"));
        assertFalse("3rd request in same window denied", limiter.tryAcquire("c2"));

        ticker.advance(Duration.ofSeconds(1));
        assertTrue("new window resets counter", limiter.tryAcquire("c2"));
    }

    /** Documents the fixed-window weakness: up to 2x permits can pass straddling a boundary. */
    private static void testFixedWindowBoundaryBurstIsAllowed() {
        // Documents the known weakness: back-to-back bursts straddling a window boundary.
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.FIXED_WINDOW_COUNTER, config, ticker);

        ticker.advance(Duration.ofMillis(999));
        assertTrue("end of window permit 1", limiter.tryAcquire("c3"));
        assertTrue("end of window permit 2", limiter.tryAcquire("c3"));

        ticker.advance(Duration.ofMillis(2)); // now in the next window
        assertTrue("start of next window permit 1", limiter.tryAcquire("c3"));
        assertTrue("start of next window permit 2", limiter.tryAcquire("c3"));
        // 4 permits granted within ~4ms even though the policy is "2 per second".
    }

    /** Verifies the sliding-window log frees a slot only when that entry's own timestamp ages out. */
    private static void testSlidingWindowLogExactAccounting() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_LOG, config, ticker);

        assertTrue("permit 1", limiter.tryAcquire("c4"));
        ticker.advance(Duration.ofMillis(500));
        assertTrue("permit 2", limiter.tryAcquire("c4"));
        assertFalse("3rd request denied (2 entries still in window)", limiter.tryAcquire("c4"));

        ticker.advance(Duration.ofMillis(501)); // first entry (t=0) now ages out at t=1001ms
        assertTrue("oldest entry aged out, 1 slot freed", limiter.tryAcquire("c4"));
        assertFalse("but the t=500ms entry is still within its own window", limiter.tryAcquire("c4"));
    }

    /** Verifies a denied request reports a retry-after equal to the full window when at limit. */
    private static void testSlidingWindowLogRetryAfterMatchesOldestEntry() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_LOG, config, ticker);

        assertTrue("first permit", limiter.tryAcquire("c5"));
        RateLimitResult denied = limiter.tryAcquireDetailed("c5");
        assertFalse("second request denied", denied.allowed());
        assertEquals("retryAfter equals full window", 1000L, denied.retryAfterMillis());
    }

    /** Verifies the sliding-window counter still throttles just after a boundary, then relaxes. */
    private static void testSlidingWindowCounterSmoothsBoundaryBurst() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_COUNTER, config, ticker);

        ticker.advance(Duration.ofMillis(999)); // near the end of window 0
        assertTrue("permit 1 at end of window 0", limiter.tryAcquire("c6"));
        assertTrue("permit 2 at end of window 0", limiter.tryAcquire("c6"));

        ticker.advance(Duration.ofMillis(2)); // now just inside window 1; overlap fraction ~1.0
        assertFalse("weighted estimate from full previous window still blocks", limiter.tryAcquire("c6"));

        ticker.advance(Duration.ofMillis(900)); // now far into window 1; overlap fraction ~0.1
        assertTrue("previous window's weight has mostly decayed", limiter.tryAcquire("c6"));
    }

    /** Verifies that one client's consumption does not affect another client's permits. */
    private static void testPerClientIsolation() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config, ticker);

        assertTrue("client X gets its own permit", limiter.tryAcquire("clientX"));
        assertTrue("client Y is unaffected by client X", limiter.tryAcquire("clientY"));
        assertFalse("client X is now exhausted", limiter.tryAcquire("clientX"));
    }

    /** Verifies each misuse path throws its specific custom exception type. */
    private static void testCustomExceptionsAreThrown() {
        assertThrows("non-positive permits config rejected", InvalidRateLimiterConfigException.class,
                () -> RateLimiterConfig.of(0, Duration.ofSeconds(1)));
        assertThrows("zero window config rejected", InvalidRateLimiterConfigException.class,
                () -> RateLimiterConfig.of(1, Duration.ZERO));

        RateLimiterConfig config = RateLimiterConfig.of(5, Duration.ofSeconds(1));
        RateLimiter tokenBucket = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config, new FakeTicker());
        assertThrows("non-positive permits request rejected", InvalidRateLimitRequestException.class,
                () -> tokenBucket.tryAcquire("c", 0));

        RateLimiter log = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_LOG, config, new FakeTicker());
        assertThrows("multi-permit on sliding window log rejected",
                UnsupportedRateLimiterOperationException.class, () -> log.tryAcquire("c", 2));
    }

    /**
     * Verifies that when far more threads than permits race for one client, exactly capacity win.
     *
     * @throws InterruptedException if the thread coordination latches are interrupted
     */
    private static void testConcurrentRaceGrantsExactlyCapacityPermits() throws InterruptedException {
        RateLimiterConfig config = RateLimiterConfig.of(10, Duration.ofSeconds(60));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config);

        int threadCount = 100;
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (limiter.tryAcquire("racer")) {
                    allowed.incrementAndGet();
                }
                done.countDown();
            }).start();
        }
        go.countDown();
        done.await();

        assertEquals("exactly capacity permits granted under contention", 10, allowed.get());
    }

    // ---- tiny assertion helpers ----

    /**
     * Passes when {@code condition} is true.
     *
     * @param label     description reported for this check
     * @param condition the value expected to be {@code true}
     */
    private static void assertTrue(String label, boolean condition) {
        report(label, condition);
    }

    /**
     * Passes when {@code condition} is false.
     *
     * @param label     description reported for this check
     * @param condition the value expected to be {@code false}
     */
    private static void assertFalse(String label, boolean condition) {
        report(label, !condition);
    }

    /**
     * Passes when {@code expected} equals {@code actual}.
     *
     * @param label    description reported for this check
     * @param expected the expected value
     * @param actual   the observed value
     */
    private static void assertEquals(String label, long expected, long actual) {
        boolean ok = expected == actual;
        report(label + " (expected=" + expected + ", actual=" + actual + ")", ok);
    }

    /**
     * Passes when running {@code action} throws an exception assignable to {@code expected}.
     *
     * @param label    description reported for this check
     * @param expected the exception type that must be thrown
     * @param action   the code expected to throw
     */
    private static void assertThrows(String label, Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
            report(label + " (expected " + expected.getSimpleName() + ", but nothing was thrown)", false);
        } catch (Throwable actual) {
            boolean ok = expected.isInstance(actual);
            report(label + " (expected " + expected.getSimpleName() + ", got "
                    + actual.getClass().getSimpleName() + ")", ok);
        }
    }

    /**
     * Records a single check's outcome, printing a PASS/FAIL line and updating the tallies.
     *
     * @param label description of the check
     * @param ok    whether the check passed
     */
    private static void report(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  [PASS] " + label);
        } else {
            failed++;
            System.out.println("  [FAIL] " + label);
        }
    }

    /** Manually-advanced fake clock: starts at an arbitrary non-zero epoch, moves only when told. */
    private static final class FakeTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong(1_000_000_000_000L);

        /**
         * Advances the fake clock by the given duration.
         *
         * @param duration how far to move time forward
         */
        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        /**
         * Returns the current fake time.
         *
         * @return the accumulated nanosecond reading
         */
        @Override
        public long nanoTime() {
            return nanos.get();
        }
    }
}
