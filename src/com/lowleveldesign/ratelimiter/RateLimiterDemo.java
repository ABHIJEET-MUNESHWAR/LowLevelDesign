package com.lowleveldesign.ratelimiter;

import com.lowleveldesign.ratelimiter.model.RateLimitResult;
import com.lowleveldesign.ratelimiter.model.RateLimiterConfig;
import com.lowleveldesign.ratelimiter.service.RateLimiter;
import com.lowleveldesign.ratelimiter.service.RateLimiterFactory;
import com.lowleveldesign.ratelimiter.service.RateLimiterType;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runnable worked example exercising all four rate limiting algorithms: burst handling, refill
 * over time, the fixed-window boundary-burst problem (and how the sliding window counter smooths
 * it), and a concurrent race for the same client's permits.
 */
public final class RateLimiterDemo {

    public static void main(String[] args) throws InterruptedException {
        tokenBucketDemo();
        fixedWindowBoundaryBurstDemo();
        slidingWindowLogDemo();
        slidingWindowCounterDemo();
        concurrentRaceDemo();
    }

    private static void tokenBucketDemo() {
        System.out.println("=== Token Bucket: 3 requests / 300ms, burst then throttle ===");
        RateLimiterConfig config = RateLimiterConfig.of(3, Duration.ofMillis(300));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config);

        for (int i = 1; i <= 4; i++) {
            RateLimitResult result = limiter.tryAcquireDetailed("client-A");
            System.out.printf("  request #%d -> allowed=%s remaining=%d retryAfterMs=%d%n",
                    i, result.allowed(), result.remainingPermits(), result.retryAfterMillis());
        }
        System.out.println();
    }

    private static void fixedWindowBoundaryBurstDemo() throws InterruptedException {
        System.out.println("=== Fixed Window Counter: boundary burst (2 permits / 200ms) ===");
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofMillis(200));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.FIXED_WINDOW_COUNTER, config);

        System.out.println("  spending both permits right away: "
                + limiter.tryAcquire("client-B") + ", " + limiter.tryAcquire("client-B"));
        System.out.println("  3rd request in same window (should deny): " + limiter.tryAcquire("client-B"));
        Thread.sleep(205); // cross into the next window
        System.out.println("  2 more requests right after window reset (fixed window allows"
                + " this back-to-back burst): "
                + limiter.tryAcquire("client-B") + ", " + limiter.tryAcquire("client-B"));
        System.out.println();
    }

    private static void slidingWindowLogDemo() throws InterruptedException {
        System.out.println("=== Sliding Window Log: exact accounting (2 permits / 200ms) ===");
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofMillis(200));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_LOG, config);

        System.out.println("  first 2 requests: " + limiter.tryAcquire("client-C") + ", "
                + limiter.tryAcquire("client-C"));
        System.out.println("  3rd request immediately (should deny): " + limiter.tryAcquire("client-C"));
        Thread.sleep(210); // both earlier timestamps age out of the trailing window
        System.out.println("  after the window fully elapses (should allow): " + limiter.tryAcquire("client-C"));
        System.out.println();
    }

    private static void slidingWindowCounterDemo() throws InterruptedException {
        System.out.println("=== Sliding Window Counter: smooths the boundary burst (2 permits / 200ms) ===");
        RateLimiterConfig config = RateLimiterConfig.of(2, Duration.ofMillis(200));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.SLIDING_WINDOW_COUNTER, config);

        System.out.println("  spend both permits: " + limiter.tryAcquire("client-D") + ", "
                + limiter.tryAcquire("client-D"));
        Thread.sleep(205); // just into the next window; previous window still weighs heavily
        System.out.println("  request right after window flip (should still be throttled by the"
                + " weighted previous-window count): " + limiter.tryAcquire("client-D"));
        System.out.println();
    }

    private static void concurrentRaceDemo() throws InterruptedException {
        System.out.println("=== Concurrent race: 20 threads, 5 permits / 1s, token bucket ===");
        RateLimiterConfig config = RateLimiterConfig.of(5, Duration.ofSeconds(1));
        RateLimiter limiter = RateLimiterFactory.create(RateLimiterType.TOKEN_BUCKET, config);

        int threadCount = 20;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (limiter.tryAcquire("client-E")) {
                    allowedCount.incrementAndGet();
                }
                done.countDown();
            });
            t.start();
        }
        ready.await();
        go.countDown();
        done.await();

        System.out.println("  threads that raced simultaneously: " + threadCount);
        System.out.println("  allowed (must be exactly 5, the bucket capacity): " + allowedCount.get());
    }
}
