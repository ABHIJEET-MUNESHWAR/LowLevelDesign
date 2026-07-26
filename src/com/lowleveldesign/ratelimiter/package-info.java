/**
 * Multi-tenant Rate Limiter -- low level design.
 *
 * <h2>Design in one line</h2>
 * A single {@code RateLimiter} instance enforces one policy ({@code N permits per window}) for
 * many independent clients at once (per API key / user id / IP), keyed in a
 * {@code ConcurrentHashMap} with each client's own state object doubling as its monitor lock --
 * so unrelated clients never contend, and four interchangeable algorithms
 * (token bucket, fixed window counter, sliding window log, sliding window counter) all speak the
 * same {@code RateLimiter} contract via a factory.
 *
 * <h2>Package map</h2>
 * <ul>
 *   <li>{@code model}   -- {@code RateLimiterConfig} (immutable "permits per window" policy),
 *                          {@code RateLimitResult} (allowed + remaining + retry-after, like real
 *                          {@code X-RateLimit-*} headers), and {@code Ticker} (pluggable time
 *                          source so tests can move time deterministically instead of sleeping).</li>
 *   <li>{@code service} -- the {@code RateLimiter} facade interface, one class per algorithm
 *                          ({@code TokenBucketRateLimiter}, {@code FixedWindowCounterRateLimiter},
 *                          {@code SlidingWindowLogRateLimiter},
 *                          {@code SlidingWindowCounterRateLimiter}), {@code RateLimiterType}, and
 *                          {@code RateLimiterFactory} to construct any of them by type.</li>
 *   <li>{@code exception} -- a {@code RateLimiterException} base type and its unchecked subtypes
 *                          ({@code InvalidRateLimiterConfigException},
 *                          {@code InvalidRateLimitRequestException},
 *                          {@code UnsupportedRateLimiterOperationException},
 *                          {@code UnknownRateLimiterTypeException}) so misuse fails with a
 *                          domain-specific type rather than a generic {@code IllegalArgumentException}.</li>
 *   <li>{@code test}    -- dependency-free correctness and concurrency tests
 *                          ({@code TestRunner}) driven by a fake {@code Ticker}.</li>
 * </ul>
 *
 * <h2>The decisions worth defending</h2>
 * <ol>
 *   <li><b>One config shape for every algorithm: "permits per window."</b> Rather than exposing
 *       algorithm-specific knobs (bucket capacity + refill rate, window size + count, etc.)
 *       separately, {@code RateLimiterConfig} always means "at most {@code permits} requests per
 *       {@code window}." Token bucket derives capacity = permits and refill rate =
 *       permits/window from it. This lets a caller swap {@code RateLimiterType} without
 *       re-deriving parameters, and makes the four algorithms genuinely comparable.</li>
 *   <li><b>Per-client state doubles as its own lock (Monitor Object), no global lock.</b> Each
 *       algorithm keeps a {@code ConcurrentHashMap<String, State>}; a request for client A never
 *       blocks a request for client B. Within one client, {@code synchronized(state)} makes the
 *       read-modify-write (check tokens / count, then consume) atomic -- the same pattern this
 *       repo's {@code meetingscheduler.Room} uses for per-room locking.</li>
 *   <li><b>Lazy, on-access refill/window-rollover instead of a background thread.</b> Token
 *       bucket computes elapsed time since last touch and tops up tokens on the calling thread;
 *       fixed/sliding window recompute the current window index on the calling thread. No
 *       scheduled task, no thread pool, no drift -- state is always correct at the instant it's
 *       read, and idle clients cost nothing.</li>
 *   <li><b>Time is injected via {@code Ticker}, not read directly from {@code System.nanoTime()}.
 *       </b> Every algorithm takes a {@code Ticker} in its constructor
 *       ({@code RateLimiterFactory} defaults to the system clock). Tests use a fake, manually
 *       advanced {@code Ticker} to assert exact window/refill boundaries without
 *       {@code Thread.sleep} flakiness.</li>
 *   <li><b>Four algorithms, one contract, explicit trade-offs:</b>
 *       <ul>
 *         <li>{@code TokenBucketRateLimiter} -- smooth, allows bounded bursts up to
 *             {@code permits}, O(1) memory/time per client. Good default for most APIs.</li>
 *         <li>{@code FixedWindowCounterRateLimiter} -- simplest and cheapest, but allows a
 *             "boundary burst" of up to {@code 2x permits} for requests straddling a window
 *             edge.</li>
 *         <li>{@code SlidingWindowLogRateLimiter} -- exact, no approximation, but O(permits)
 *             memory per client (keeps a timestamp per accepted request).</li>
 *         <li>{@code SlidingWindowCounterRateLimiter} -- O(1) memory approximation that smooths
 *             the fixed window's boundary burst via a linearly-decaying weight on the previous
 *             window's count. The accuracy/memory sweet spot for high-{@code permits} limits.</li>
 *       </ul>
 *   </li>
 *   <li><b>Single-process, not distributed.</b> State lives in memory per {@code RateLimiter}
 *       instance -- correct for a single host or a sticky-routed service. Scaling across many
 *       hosts behind a shared limit requires moving the per-client counters to a shared store
 *       (e.g. Redis {@code INCR}/Lua script for fixed window, or a Redis sorted set for sliding
 *       window log) and replacing the in-process lock with an atomic remote operation -- the
 *       algorithms' logic ports over unchanged.</li>
 * </ol>
 *
 * <p>Run {@code RateLimiterDemo} for a worked scenario across all four algorithms (including the
 * fixed-window boundary burst and a 20-thread race for one client's token bucket) and
 * {@code test.TestRunner} for the correctness and concurrency suite.
 */
package com.lowleveldesign.ratelimiter;
