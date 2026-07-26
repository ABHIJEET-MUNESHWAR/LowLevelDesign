package com.lowleveldesign.ratelimiter.model;

/**
 * Immutable outcome of a rate-limit decision, richer than a bare boolean. Mirrors what
 * production gateways return via headers such as {@code X-RateLimit-Remaining} and
 * {@code Retry-After}.
 *
 * <ul>
 *   <li>{@code allowed}          -- whether the request may proceed</li>
 *   <li>{@code remainingPermits} -- best-effort estimate of permits left in the current
 *                                   window/bucket</li>
 *   <li>{@code retryAfterMillis} -- if denied, a hint for how long the caller should wait
 *                                   before retrying; {@code 0} if allowed</li>
 * </ul>
 */
public final class RateLimitResult {

    private final boolean allowed;
    private final long remainingPermits;
    private final long retryAfterMillis;

    /**
     * Creates a result directly. Prefer the {@link #allow(long)} / {@link #deny(long)} factories
     * for readability.
     *
     * @param allowed          whether the request may proceed
     * @param remainingPermits estimated permits left in the current window/bucket
     * @param retryAfterMillis  suggested wait before retrying, in milliseconds ({@code 0} if allowed)
     */
    public RateLimitResult(boolean allowed, long remainingPermits, long retryAfterMillis) {
        this.allowed = allowed;
        this.remainingPermits = remainingPermits;
        this.retryAfterMillis = retryAfterMillis;
    }

    /**
     * Builds an "allowed" result with no retry-after.
     *
     * @param remainingPermits estimated permits still available after this request
     * @return a result with {@code allowed == true}
     */
    public static RateLimitResult allow(long remainingPermits) {
        return new RateLimitResult(true, remainingPermits, 0L);
    }

    /**
     * Builds a "denied" result carrying a retry-after hint.
     *
     * @param retryAfterMillis suggested wait before retrying; clamped to a minimum of {@code 0}
     * @return a result with {@code allowed == false} and zero remaining permits
     */
    public static RateLimitResult deny(long retryAfterMillis) {
        return new RateLimitResult(false, 0L, Math.max(retryAfterMillis, 0L));
    }

    /**
     * Reports whether the request was permitted.
     *
     * @return {@code true} if the caller may proceed, {@code false} if it should be throttled
     */
    public boolean allowed() {
        return allowed;
    }

    /**
     * Returns the best-effort estimate of permits remaining in the current window/bucket, suitable
     * for an {@code X-RateLimit-Remaining} header.
     *
     * @return the estimated remaining permit count ({@code 0} when denied)
     */
    public long remainingPermits() {
        return remainingPermits;
    }

    /**
     * Returns the suggested wait before retrying, suitable for a {@code Retry-After} header.
     *
     * @return milliseconds the caller should wait before retrying ({@code 0} when allowed)
     */
    public long retryAfterMillis() {
        return retryAfterMillis;
    }

    /**
     * Returns a diagnostic string containing all three fields.
     *
     * @return a debug representation of this result
     */
    @Override
    public String toString() {
        return "RateLimitResult{allowed=" + allowed + ", remainingPermits=" + remainingPermits
                + ", retryAfterMillis=" + retryAfterMillis + '}';
    }
}
