package com.miniakvaryum.domain.feature.bedtime;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Tunable times of the bedtime feature.
 *
 * @param start               bedtime starts (fish sleep, minigames and feeding closed)
 * @param end                 bedtime ends (good morning)
 * @param morningGreetingUntil the morning greeting is only shown when the app is opened before this time
 */
public record BedtimeConfig(LocalTime start, LocalTime end, LocalTime morningGreetingUntil) {

    public BedtimeConfig {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(morningGreetingUntil, "morningGreetingUntil");
        if (!end.isBefore(start)) {
            throw new IllegalArgumentException("bedtime must cross midnight: end before start");
        }
        if (morningGreetingUntil.isBefore(end) || !morningGreetingUntil.isBefore(start)) {
            throw new IllegalArgumentException("morningGreetingUntil must lie between end and start");
        }
    }

    /** The plan values: bedtime from 21:00 to 07:00, morning greeting until 12:00. */
    public static BedtimeConfig defaults() {
        return new BedtimeConfig(LocalTime.of(21, 0), LocalTime.of(7, 0), LocalTime.of(12, 0));
    }
}
