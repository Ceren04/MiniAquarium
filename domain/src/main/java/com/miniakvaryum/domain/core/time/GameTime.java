package com.miniakvaryum.domain.core.time;

import java.util.Objects;
import java.util.Optional;

/**
 * Trusted game time: a millisecond counter that starts at 0 in a new game and can only be
 * moved forward by time that really passed. Changing the device date gives no advantage.
 *
 * <p>Everything time-based in the domain (PIN lockout, fish age, ledger timestamps) reads
 * {@link #nowMillis()} and never the device clock.
 *
 * <p>How it works:
 * <ul>
 *   <li><b>While the game runs</b>, game time follows the monotonic clock only.</li>
 *   <li><b>Between two sessions</b>, {@link #startSession} decides how much offline time to
 *       credit. If the device was not restarted, the monotonic clock is used and the wall
 *       clock is ignored. Otherwise the wall clock is the only information available, so its
 *       difference is used, but never less than 0 and never more than {@code maxGapMillis}.</li>
 * </ul>
 *
 * <p>Usage: create it, call {@link #startSession} exactly once, then read {@link #nowMillis()}.
 * Store {@link #anchor()} in the save file.
 */
public final class GameTime {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final Clock clock;
    private final BootIdProvider boot;
    private final long maxGapMillis;

    private boolean started;
    private long gameMillisAtStart;
    private long monotonicAtStart;

    /**
     * @param clock        device clocks
     * @param boot         boot id of the device
     * @param maxGapMillis the most offline time one session start may credit (the 24 h cap)
     */
    public GameTime(Clock clock, BootIdProvider boot, long maxGapMillis) {
        if (maxGapMillis < 0) {
            throw new IllegalArgumentException("maxGapMillis must be >= 0 but was " + maxGapMillis);
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.boot = Objects.requireNonNull(boot, "boot");
        this.maxGapMillis = maxGapMillis;
    }

    /**
     * Starts the session and works out the credited offline time.
     *
     * @param saved the anchor from the save file, or empty for a new game
     * @return the offline time that was added to the game time
     * @throws IllegalStateException if called more than once
     */
    public CreditedGap startSession(Optional<TimeAnchor> saved) {
        if (started) {
            throw new IllegalStateException("startSession must be called exactly once");
        }
        long monotonicNow = clock.monotonicNanos();
        CreditedGap gap = saved.map(anchor -> measureGap(anchor, monotonicNow))
                .orElse(new CreditedGap(0, GapSource.FIRST_RUN, false));

        gameMillisAtStart = saved.map(TimeAnchor::gameMillis).orElse(0L) + gap.millis();
        monotonicAtStart = monotonicNow;
        started = true;
        return gap;
    }

    private CreditedGap measureGap(TimeAnchor saved, long monotonicNow) {
        // Same boot: both monotonic readings come from the same counter, so their
        // difference is real elapsed time. The wall clock is not looked at.
        boolean sameBoot = !saved.bootId().isEmpty()
                && saved.bootId().equals(currentBootId())
                && monotonicNow >= saved.monotonicNanos();

        long raw;
        GapSource source;
        if (sameBoot) {
            raw = (monotonicNow - saved.monotonicNanos()) / NANOS_PER_MILLI;
            source = GapSource.MONOTONIC;
        } else {
            // Different boot or unknown boot id: the monotonic counter has restarted,
            // so the wall clock is all we have. A negative difference means the date
            // was set back; that earns nothing.
            raw = clock.wallMillis() - saved.wallMillis();
            if (raw < 0) {
                raw = 0;
                source = GapSource.WALL_NEGATIVE_CLAMPED;
            } else {
                source = GapSource.WALL;
            }
        }
        // The cap limits what setting the date forward can earn.
        return new CreditedGap(Math.min(raw, maxGapMillis), source, raw > maxGapMillis);
    }

    /**
     * Returns the game time now, in milliseconds.
     *
     * @throws IllegalStateException if the session has not been started
     */
    public long nowMillis() {
        requireStarted();
        return gameMillisAtStart + (clock.monotonicNanos() - monotonicAtStart) / NANOS_PER_MILLI;
    }

    /**
     * Returns a snapshot of the game time and all clocks, to be stored in the save file.
     *
     * @throws IllegalStateException if the session has not been started
     */
    public TimeAnchor anchor() {
        requireStarted();
        return new TimeAnchor(nowMillis(), clock.monotonicNanos(), clock.wallMillis(), currentBootId());
    }

    private String currentBootId() {
        return boot.currentBootId().orElse("");
    }

    private void requireStarted() {
        if (!started) {
            throw new IllegalStateException("startSession has not been called yet");
        }
    }
}
