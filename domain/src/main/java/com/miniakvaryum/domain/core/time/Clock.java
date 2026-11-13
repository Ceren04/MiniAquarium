package com.miniakvaryum.domain.core.time;

/**
 * Access to the two clocks of the device. The domain never reads the system time directly;
 * only the platform implementation of this interface (in the desktop module) does.
 *
 * <p>The two clocks behave differently, and trusted game time relies on that difference:
 * <ul>
 *   <li>The <b>wall clock</b> is the date and time the user sees. The user can change it.</li>
 *   <li>The <b>monotonic clock</b> only counts forward while the device is running. The user
 *       cannot change it, but it restarts from an arbitrary value after every reboot.</li>
 * </ul>
 */
public interface Clock {

    /** Wall-clock time in milliseconds since 1970-01-01 UTC. Can jump when the user changes the date. */
    long wallMillis();

    /** Monotonic time in nanoseconds. Only differences between two readings are meaningful. */
    long monotonicNanos();
}
