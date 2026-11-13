package com.miniakvaryum.domain.core.time;

/** Says which clock was used to measure the time between two sessions. */
public enum GapSource {

    /** There was no save file: this is the first session ever. */
    FIRST_RUN,

    /** Same boot as the previous session: measured with the monotonic clock (cannot be cheated). */
    MONOTONIC,

    /** Device was restarted or the boot id is unknown: measured with the wall clock. */
    WALL,

    /** The wall clock went backwards since the last session: no time is credited. */
    WALL_NEGATIVE_CLAMPED
}
