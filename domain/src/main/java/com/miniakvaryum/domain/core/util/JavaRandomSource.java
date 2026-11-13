package com.miniakvaryum.domain.core.util;

import java.util.Random;

/**
 * Production {@link RandomSource} backed by {@link java.util.Random}.
 *
 * <p>This is the only class in the domain that may create a {@code Random}.
 * The same seed always produces the same sequence of values.
 */
public final class JavaRandomSource implements RandomSource {

    private final Random random;

    /** Creates a source that starts from the given seed. */
    public JavaRandomSource(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be > 0 but was " + bound);
        }
        return random.nextInt(bound);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
