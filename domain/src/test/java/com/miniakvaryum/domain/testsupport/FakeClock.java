package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.time.Clock;

/**
 * A {@link Clock} whose two readings are set by the test. Time never moves on its own,
 * so tests need no {@code Thread.sleep} and always produce the same result.
 */
public final class FakeClock implements Clock {

    private long wallMillis;
    private long monotonicNanos;

    public FakeClock(long wallMillis, long monotonicNanos) {
        this.wallMillis = wallMillis;
        this.monotonicNanos = monotonicNanos;
    }

    /** Real time passes: both clocks move forward by the same amount. */
    public void advanceMillis(long millis) {
        wallMillis += millis;
        monotonicNanos += millis * 1_000_000L;
    }

    /** The user changes the device date: only the wall clock jumps (forward or backward). */
    public void shiftWallMillis(long millis) {
        wallMillis += millis;
    }

    public void setWallMillis(long wallMillis) {
        this.wallMillis = wallMillis;
    }

    /** Simulates a reboot or any other situation where the monotonic clock restarts. */
    public void setMonotonicNanos(long monotonicNanos) {
        this.monotonicNanos = monotonicNanos;
    }

    @Override
    public long wallMillis() {
        return wallMillis;
    }

    @Override
    public long monotonicNanos() {
        return monotonicNanos;
    }
}
