package com.miniakvaryum.domain.core.session;

/**
 * Tunable numbers of the core. Each feature has its own config record next to its code;
 * only values that belong to no feature live here.
 *
 * @param maxGapMillis      the most offline time one start-up may credit
 * @param autosaveIntervalS seconds between two automatic saves
 */
public record CoreConfig(long maxGapMillis, double autosaveIntervalS) {

    public CoreConfig {
        if (maxGapMillis < 0) {
            throw new IllegalArgumentException("maxGapMillis must be >= 0");
        }
        if (autosaveIntervalS <= 0) {
            throw new IllegalArgumentException("autosaveIntervalS must be > 0");
        }
    }

    /** The values from the specification: 24 hours and 30 seconds. */
    public static CoreConfig defaults() {
        return new CoreConfig(86_400_000L, 30.0);
    }
}
