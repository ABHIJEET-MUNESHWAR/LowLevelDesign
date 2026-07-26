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

    public RateLimitResult(boolean allowed, long remainingPermits, long retryAfterMillis) {
        this.allowed = allowed;
        this.remainingPermits = remainingPermits;
        this.retryAfterMillis = retryAfterMillis;
    }

    public static RateLimitResult allow(long remainingPermits) {
        return new RateLimitResult(true, remainingPermits, 0L);
    }

    public static RateLimitResult deny(long retryAfterMillis) {
        return new RateLimitResult(false, 0L, Math.max(retryAfterMillis, 0L));
    }

    public boolean allowed() {
        return allowed;
    }

    public long remainingPermits() {
        return remainingPermits;
    }

    public long retryAfterMillis() {
        return retryAfterMillis;
    }

    @Override
    public String toString() {
        return "RateLimitResult{allowed=" + allowed + ", remainingPermits=" + remainingPermits
                + ", retryAfterMillis=" + retryAfterMillis + '}';
    }
}
