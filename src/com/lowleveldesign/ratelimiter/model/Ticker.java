package com.lowleveldesign.ratelimiter.model;

/**
 * Pluggable time source. Production code uses {@link #systemTicker()} ({@code System.nanoTime()});
 * tests use a manually-advanced fake so window/refill boundaries can be asserted deterministically
 * instead of relying on {@code Thread.sleep} and hoping for the best.
 */
@FunctionalInterface
public interface Ticker {

    long nanoTime();

    static Ticker systemTicker() {
        return System::nanoTime;
    }
}
