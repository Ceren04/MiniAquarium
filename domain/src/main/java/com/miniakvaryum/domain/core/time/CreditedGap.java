package com.miniakvaryum.domain.core.time;

/**
 * The offline time the game accepts as "really passed" between the previous session and this one.
 * Features use it at start-up for everything that happens while the app is closed
 * (fish age, and later dirt and energy).
 *
 * @param millis credited time in milliseconds, never negative, never above the configured cap
 * @param source which clock produced the value
 * @param capped true if the measured time was longer than the cap and was cut down
 */
public record CreditedGap(long millis, GapSource source, boolean capped) {
}
