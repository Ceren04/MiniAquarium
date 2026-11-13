package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.util.RandomSource;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A {@link RandomSource} that returns exactly the values the test scripted, in order.
 *
 * <p>Example: {@code new ScriptedRandom().ints(3).doubles(0.25)} makes the first
 * {@code nextInt} call return 3 and the first {@code nextDouble} call return 0.25.
 * Asking for more values than were scripted fails the test, so a test notices when the
 * code under test draws a different number of random values than expected.
 */
public final class ScriptedRandom implements RandomSource {

    private final Deque<Integer> ints = new ArrayDeque<>();
    private final Deque<Double> doubles = new ArrayDeque<>();

    /** Appends values for upcoming {@link #nextInt(int)} calls. */
    public ScriptedRandom ints(int... values) {
        for (int v : values) {
            ints.add(v);
        }
        return this;
    }

    /** Appends values for upcoming {@link #nextDouble()} calls. */
    public ScriptedRandom doubles(double... values) {
        for (double v : values) {
            doubles.add(v);
        }
        return this;
    }

    @Override
    public int nextInt(int bound) {
        if (ints.isEmpty()) {
            throw new IllegalStateException("ScriptedRandom has no more int values");
        }
        int value = ints.poll();
        if (value < 0 || value >= bound) {
            throw new IllegalStateException("Scripted int " + value + " is outside [0, " + bound + ")");
        }
        return value;
    }

    @Override
    public double nextDouble() {
        if (doubles.isEmpty()) {
            throw new IllegalStateException("ScriptedRandom has no more double values");
        }
        return doubles.poll();
    }
}
