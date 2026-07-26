package com.lowleveldesign.ratelimiter.test;

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

    public static void main(String[] args) throws InterruptedException {
        testTokenBucketBurstThenThrottleThenRefill();
        testFixedWindowResetsOnBoundary();
        testFixedWindowBoundaryBurstIsAllowed();
        testSlidingWindowLogExactAccounting();
        testSlidingWindowLogRetryAfterMatchesOldestEntry();
        testSlidingWindowCounterSmoothsBoundaryBurst();
        testPerClientIsolation();
        testConcurrentRaceGrantsExactlyCapacityPermits();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

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

    private static void testSlidingWindowLogRetryAfterMatchesOldestEntry() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_LOG, config, ticker);

        assertTrue("first permit", limiter.tryAcquire("c5"));
        RateLimitResult denied = limiter.tryAcquireDetailed("c5");
        assertFalse("second request denied", denied.allowed());
        assertEquals("retryAfter equals full window", 1000L, denied.retryAfterMillis());
    }

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

    private static void testPerClientIsolation() {
        FakeTicker ticker = new FakeTicker();
        RateLimiterConfig config = RateLimiterConfig.of(1, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config, ticker);

        assertTrue("client X gets its own permit", limiter.tryAcquire("clientX"));
        assertTrue("client Y is unaffected by client X", limiter.tryAcquire("clientY"));
        assertFalse("client X is now exhausted", limiter.tryAcquire("clientX"));
    }

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

    private static void assertTrue(String label, boolean condition) {
        report(label, condition);
    }

    private static void assertFalse(String label, boolean condition) {
        report(label, !condition);
    }

    private static void assertEquals(String label, long expected, long actual) {
        boolean ok = expected == actual;
        report(label + " (expected=" + expected + ", actual=" + actual + ")", ok);
    }

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

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }

        @Override
        public long nanoTime() {
            return nanos.get();
        }
    }
}
