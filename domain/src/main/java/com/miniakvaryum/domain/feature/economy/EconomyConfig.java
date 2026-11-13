package com.miniakvaryum.domain.feature.economy;

/**
 * Tunable numbers of the economy feature.
 *
 * @param initialFood  food a new game starts with
 * @param initialCoins coins a new game starts with
 */
public record EconomyConfig(long initialFood, long initialCoins) {

    public EconomyConfig {
        if (initialFood < 0 || initialCoins < 0) {
            throw new IllegalArgumentException("initial amounts must not be negative");
        }
    }

    /** The values from the specification: 5 food, 0 coins. */
    public static EconomyConfig defaults() {
        return new EconomyConfig(5, 0);
    }
}
