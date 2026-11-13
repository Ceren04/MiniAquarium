package com.miniakvaryum.domain.feature.aquarium;

/**
 * Tunable numbers of fish behavior and growth.
 *
 * @param initialFishCount fish in a new game
 * @param detectRange      a fish notices a free pellet within this distance
 * @param eatRange         a fish can start eating a pellet within this distance
 * @param slowSpeed        speed of SLOW_MOVE in units per second
 * @param dashSpeed        speed of DASH in units per second
 * @param slowDurationS    duration of one SLOW_MOVE in seconds
 * @param dashDurationS    duration of one DASH in seconds
 * @param idleMinS         shortest IDLE in seconds
 * @param idleMaxS         longest IDLE in seconds
 * @param eatDurationS     duration of EAT in seconds
 * @param level2Feeds      feedings needed for level 2
 * @param level2Hours      hours in the aquarium needed for level 2 (both conditions must hold)
 * @param level3Feeds      feedings needed for level 3
 * @param level3Hours      hours in the aquarium needed for level 3 (both conditions must hold)
 * @param weights          action weights per situation
 */
public record FishConfig(int initialFishCount, double detectRange, double eatRange,
        double slowSpeed, double dashSpeed, double slowDurationS, double dashDurationS,
        double idleMinS, double idleMaxS, double eatDurationS,
        int level2Feeds, int level2Hours, int level3Feeds, int level3Hours, ActionWeights weights) {

    /** The values from the specification. */
    public static FishConfig defaults() {
        return new FishConfig(3, 300, 70, 40, 160, 3.0, 1.0, 1.0, 2.5, 1.5, 20, 48, 100, 300,
                ActionWeights.defaults());
    }
}
