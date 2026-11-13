package com.miniakvaryum.desktop;

import com.miniakvaryum.domain.core.time.Clock;

/**
 * The real clocks of the computer.
 *
 * <p>This is the ONLY class in the whole project that may call
 * {@code System.currentTimeMillis()} and {@code System.nanoTime()}.
 */
public final class SystemClock implements Clock {

    @Override
    public long wallMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public long monotonicNanos() {
        return System.nanoTime();
    }
}
