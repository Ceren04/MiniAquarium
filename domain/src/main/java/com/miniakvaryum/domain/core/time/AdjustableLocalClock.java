package com.miniakvaryum.domain.core.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * The {@link LocalClock} of the game: the device wall clock in the device time zone, which the
 * developer tools can shift to try bedtime without changing the real device clock.
 *
 * <p>The game creates one instance at start-up and keeps it, so a shift survives the restarts
 * of the developer time travel. A shift is never saved.
 */
public final class AdjustableLocalClock implements LocalClock {

    private final Clock wall;
    private final ZoneId zone;
    private long offsetMillis;

    /**
     * @param wall the device clocks
     * @param zone time zone of the device
     */
    public AdjustableLocalClock(Clock wall, ZoneId zone) {
        this.wall = Objects.requireNonNull(wall, "wall");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    /** The device clock in the device's own time zone. */
    public static AdjustableLocalClock ofSystemZone(Clock wall) {
        return new AdjustableLocalClock(wall, ZoneId.systemDefault());
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(wall.wallMillis() + offsetMillis), zone);
    }

    /**
     * <b>Developer tools only.</b> From now on the clock shows the given time of day today and
     * runs on normally from there.
     */
    public void setTimeOfDayForDevelopment(LocalTime time) {
        LocalDateTime real = LocalDateTime.ofInstant(Instant.ofEpochMilli(wall.wallMillis()), zone);
        offsetMillis = Duration.between(real, real.toLocalDate().atTime(time)).toMillis();
    }

    /** <b>Developer tools only.</b> Back to the real device time. */
    public void resetForDevelopment() {
        offsetMillis = 0;
    }

    /** Returns true while the developer tools have shifted the clock. */
    public boolean isShifted() {
        return offsetMillis != 0;
    }
}
