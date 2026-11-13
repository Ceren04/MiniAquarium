package com.miniakvaryum.domain.core.time;

import java.util.Optional;

/**
 * Identifies the current boot (the period since the device was last switched on).
 *
 * <p>A monotonic reading from a previous session can only be compared with the current one if
 * the device has not been restarted in between. Two equal boot ids prove exactly that.
 */
public interface BootIdProvider {

    /** Returns the id of the current boot, or empty if the platform cannot provide one. */
    Optional<String> currentBootId();
}
