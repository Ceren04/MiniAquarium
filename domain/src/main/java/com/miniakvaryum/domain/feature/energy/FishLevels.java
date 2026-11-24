package com.miniakvaryum.domain.feature.energy;

import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;

/**
 * Where the energy feature gets the level of a fish from.
 *
 * <p>Fish levels ({@code LevelRules.levelOf}) are not written yet (package V10, see D-030), so
 * the game uses {@link #ALL_LEVEL_ONE}. When levels exist, only the line in
 * {@code DomainFeatures} that creates the energy feature changes.
 */
@FunctionalInterface
public interface FishLevels {

    /** Every fish counts as level 1. */
    FishLevels ALL_LEVEL_ONE = fish -> 1;

    /** Level of the fish, 1 or more. */
    int levelOf(FishData fish);
}
