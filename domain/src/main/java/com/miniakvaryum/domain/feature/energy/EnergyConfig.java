package com.miniakvaryum.domain.feature.energy;

/**
 * Tunable numbers of the energy feature.
 *
 * <p>Energy per hour = {@code base} + for every active fish ({@code perFish} +
 * {@code perLevel} x level).
 *
 * @param base       energy per hour even with an empty aquarium, so the game never locks up
 * @param perFish    energy per hour for every active fish
 * @param perLevel   extra energy per hour for every level of every active fish
 * @param startValue energy of a new game
 * @param max        highest energy; more is lost
 * @param gameCost   energy one minigame costs (used by the minigames, kept here so all energy
 *                   numbers are in one place)
 */
public record EnergyConfig(double base, double perFish, double perLevel, double startValue, double max, int gameCost) {

    public EnergyConfig {
        if (base < 0 || perFish < 0 || perLevel < 0) {
            throw new IllegalArgumentException("rates must be >= 0");
        }
        if (max <= 0 || startValue < 0 || startValue > max) {
            throw new IllegalArgumentException("need 0 <= startValue <= max and max > 0");
        }
        if (gameCost < 1 || gameCost > max) {
            throw new IllegalArgumentException("gameCost must be between 1 and max");
        }
    }

    /**
     * The values of the plan "Energie und Schlafenszeit": 1 + (2 + level) per fish and hour, so
     * 3 fish of level 1 give 10 per hour; a new game starts full (100); a game costs 20.
     */
    public static EnergyConfig defaults() {
        return new EnergyConfig(1.0, 2.0, 1.0, 100.0, 100.0, 20);
    }
}
