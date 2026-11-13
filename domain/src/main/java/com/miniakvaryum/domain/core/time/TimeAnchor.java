package com.miniakvaryum.domain.core.time;

/**
 * A snapshot of all clocks at one moment. It is written into the save file, and the next
 * session uses it to work out how much time passed while the game was closed.
 *
 * @param gameMillis     trusted game time at the moment of the snapshot
 * @param monotonicNanos monotonic clock reading at that moment
 * @param wallMillis     wall clock reading at that moment
 * @param bootId         boot id at that moment, or "" if the platform could not provide one
 */
public record TimeAnchor(long gameMillis, long monotonicNanos, long wallMillis, String bootId) {
}
