package com.miniakvaryum.domain.feature.aquarium;

import java.util.Objects;

/**
 * All tunable numbers of the aquarium feature, grouped by topic.
 *
 * @param world size of the aquarium
 * @param fish  fish behavior and growth
 * @param food  pellet behavior
 */
public record AquariumConfig(WorldConfig world, FishConfig fish, FoodConfig food) {

    public AquariumConfig {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(fish, "fish");
        Objects.requireNonNull(food, "food");
    }

    /** The values from the specification. */
    public static AquariumConfig defaults() {
        return new AquariumConfig(WorldConfig.defaults(), FishConfig.defaults(), FoodConfig.defaults());
    }
}
