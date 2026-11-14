package com.miniakvaryum.domain.feature.aquarium;

/**
 * Tunable numbers of food pellets.
 *
 * @param pelletStartY    y at which a dropped pellet appears
 * @param pelletSinkSpeed sinking speed in units per second
 * @param pelletLifetimeS a pellet nobody claimed dissolves after this many seconds (costs nothing)
 * @param claimTimeoutS   safety net: a claim older than this is released again
 * @param maxPelletsInWater most pellets that may be in the water at the same time

 */
public record FoodConfig(double pelletStartY, double pelletSinkSpeed, double pelletLifetimeS, double claimTimeoutS, int maxPelletsInWater) {

    /** The values from the specification. */
    public static FoodConfig defaults() {
        return new FoodConfig(620, 30, 60, 3.5, 5);
    }
}
