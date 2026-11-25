package com.miniakvaryum.domain.feature.trophies.api;

/** State of one trophy. */
public enum TrophyState {
    /** Not enough approved tasks yet. */
    LOCKED,
    /** Reached; the present waits for the child to tap it. */
    READY,
    /** The child opened it; the reward was given. */
    CLAIMED
}
