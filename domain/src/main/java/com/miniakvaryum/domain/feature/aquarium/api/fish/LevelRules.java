package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.feature.aquarium.FishConfig;

/**
 * Works out the level of a fish. The level is derived from feed count and age every time it is
 * needed; it is never stored.
 *
 * <p><b>SKELETON</b> (specification section 7.7). Tests: {@code LevelRulesTest}.
 */
public final class LevelRules {

    private LevelRules() {
    }

    /**
     * Returns the level (1, 2 or 3) of a fish.
     *
     * <p>TODO: hours = {@code aquariumMillis / 3_600_000}. Level 3 if
     * {@code feedCount >= level3Feeds} AND {@code hours >= level3Hours}; otherwise level 2 if
     * {@code feedCount >= level2Feeds} AND {@code hours >= level2Hours}; otherwise level 1.
     */
    public static int levelOf(int feedCount, long aquariumMillis, FishConfig config) {
        throw new UnsupportedOperationException("TODO: LevelRules.levelOf is not implemented yet");
    }
}
