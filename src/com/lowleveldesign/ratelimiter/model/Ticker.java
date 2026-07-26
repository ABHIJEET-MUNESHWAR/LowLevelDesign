package com.lowleveldesign.ratelimiter.model;

/**
 * Pluggable time source. Production code uses {@link #systemTicker()} ({@code System.nanoTime()});
 * tests use a manually-advanced fake so window/refill boundaries can be asserted deterministically
 * instead of relying on {@code Thread.sleep} and hoping for the best.
 */
@FunctionalInterface
public interface Ticker {

    /**
     * Returns the current value of a monotonic nanosecond timer. Only <em>differences</em> between
     * successive readings are meaningful; the absolute value has no defined epoch.
     *
     * @return the current reading in nanoseconds
     */
    long nanoTime();

    /**
     * Returns the production ticker backed by {@link System#nanoTime()}.
     *
     * @return a ticker that reads the JVM's monotonic clock
     */
    static Ticker systemTicker() {
        return System::nanoTime;
    }
}
