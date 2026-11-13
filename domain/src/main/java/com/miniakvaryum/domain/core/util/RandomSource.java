package com.miniakvaryum.domain.core.util;

/**
 * The only source of randomness in the domain.
 *
 * <p>Game rules never call {@code Math.random()} or {@code new Random()} directly. They ask a
 * RandomSource instead. Production uses {@link JavaRandomSource}; tests use a scripted
 * implementation that returns chosen values, so "random" behavior becomes reproducible.
 */
public interface RandomSource {

    /**
     * Returns a whole number from 0 (inclusive) to {@code bound} (exclusive).
     *
     * @param bound must be greater than 0
     */
    int nextInt(int bound);

    /** Returns a number from 0.0 (inclusive) to 1.0 (exclusive). */
    double nextDouble();
}
