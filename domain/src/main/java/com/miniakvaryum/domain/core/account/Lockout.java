package com.miniakvaryum.domain.core.account;

import java.util.List;

/**
 * Counter of wrong answers with growing waiting times, used for sign-in and for the safety
 * color. Immutable: every method returns the new state.
 *
 * @param failed          wrong answers since the last lock or success
 * @param stage           number of locks so far; selects the waiting time
 * @param lockedAtMillis  wall-clock time when the current lock started, 0 if never locked
 * @param lockedUntilMillis wall-clock time when the current lock ends, 0 if never locked
 */
public record Lockout(int failed, int stage, long lockedAtMillis, long lockedUntilMillis) {

    /** No wrong answers, not locked. */
    public static final Lockout NONE = new Lockout(0, 0, 0L, 0L);

    public Lockout {
        if (failed < 0 || stage < 0) {
            throw new IllegalArgumentException("failed and stage must be >= 0");
        }
    }

    /**
     * Milliseconds until the lock ends, 0 if not locked.
     *
     * <p>If the device clock was set back to before the start of the lock, the lock counts as
     * just started, so turning the clock back cannot shorten it.
     */
    public long remainingMillis(long nowMillis) {
        if (lockedUntilMillis <= 0) {
            return 0L;
        }
        if (nowMillis < lockedAtMillis) {
            return lockedUntilMillis - lockedAtMillis;
        }
        return Math.max(0L, lockedUntilMillis - nowMillis);
    }

    /**
     * Records one wrong answer. After {@code maxFailed} wrong answers in a row the lock starts;
     * its length is {@code waitSeconds[stage]}, the last value repeating.
     */
    public Lockout wrong(long nowMillis, int maxFailed, List<Integer> waitSeconds) {
        int newFailed = failed + 1;
        if (newFailed < maxFailed) {
            return new Lockout(newFailed, stage, lockedAtMillis, lockedUntilMillis);
        }
        int seconds = waitSeconds.get(Math.min(stage, waitSeconds.size() - 1));
        return new Lockout(0, stage + 1, nowMillis, nowMillis + seconds * 1_000L);
    }

    /** Attempts left before the next lock. */
    public int attemptsLeft(int maxFailed) {
        return Math.max(0, maxFailed - failed);
    }
}
