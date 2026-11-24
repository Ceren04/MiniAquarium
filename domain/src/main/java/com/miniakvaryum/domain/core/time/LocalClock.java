package com.miniakvaryum.domain.core.time;

import java.time.LocalDateTime;

/**
 * The local date and time of the device ("what time is it?"), for rules that depend on the
 * hour of the day, such as bedtime.
 *
 * <p>Unlike {@link GameTime}, this cannot be trusted: the user can change the device clock.
 * Use it only where the time of day matters, never to measure how much time has passed.
 */
@FunctionalInterface
public interface LocalClock {

    /** The current local date and time. */
    LocalDateTime now();
}
