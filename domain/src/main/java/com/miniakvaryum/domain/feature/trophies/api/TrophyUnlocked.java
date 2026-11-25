package com.miniakvaryum.domain.feature.trophies.api;

/** An approved task reached a new trophy; it is ready to be collected. Used for "New trophy!". */
public record TrophyUnlocked(Milestone milestone) {
}
