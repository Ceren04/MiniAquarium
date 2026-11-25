package com.miniakvaryum.domain.feature.trophies.api;

/** Result of {@link TrophyService#claim(int)}. */
public enum ClaimResult {
    /** The reward was given and the trophy is collected. */
    OK,
    /** The trophy is still locked; nothing changed. */
    NOT_READY,
    /** The trophy was collected before; nothing changed. */
    ALREADY_CLAIMED
}
