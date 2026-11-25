package com.miniakvaryum.domain.feature.bedtime.api;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.time.LocalClock;
import com.miniakvaryum.domain.feature.bedtime.BedtimeConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Bedtime from 21:00 to 07:00: the fish sleep, minigames and feeding are closed.
 *
 * <p>This service only answers "is it bedtime now?" and tells when it starts and ends
 * ({@link BedtimeStarted}, {@link BedtimeEnded}). What sleeps and what is closed is decided by
 * the features that ask: the aquarium (fish, feeding) and the minigames.
 *
 * <p>The greetings ("Goodnight, little fish!", "Good morning, little fish!") are shown once per
 * night and once per morning; {@link #consumeGoodnight()} and {@link #consumeGoodMorning()}
 * return true only the first time and remember that in the save.
 *
 * <p>Known limit: bedtime uses the device clock, which the user can change.
 */
public final class BedtimeService {

    private final BedtimeConfig config;
    private final LocalClock clock;
    private final EventBus events;
    private final Runnable onChange;
    private LocalDate goodnightShownFor;
    private LocalDate goodMorningShownFor;
    private boolean wasBedtime;

    /**
     * @param goodnightShownFor   the night (date of its evening) whose greeting was shown, or null
     * @param goodMorningShownFor the day whose morning greeting was shown, or null
     * @param onChange            asks for a save
     */
    public BedtimeService(BedtimeConfig config, LocalClock clock, EventBus events, Runnable onChange,
            LocalDate goodnightShownFor, LocalDate goodMorningShownFor) {
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.events = Objects.requireNonNull(events, "events");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.goodnightShownFor = goodnightShownFor;
        this.goodMorningShownFor = goodMorningShownFor;
        this.wasBedtime = isBedtime();
    }

    /** Returns true between {@code start} (21:00) and {@code end} (07:00). */
    public boolean isBedtime() {
        return isBedtimeAt(clock.now().toLocalTime());
    }

    private boolean isBedtimeAt(LocalTime time) {
        return !time.isBefore(config.start()) || time.isBefore(config.end());
    }

    /** When the current bedtime ends (the next 07:00), or empty if it is not bedtime. */
    public Optional<LocalDateTime> bedtimeEndsAt() {
        LocalDateTime now = clock.now();
        if (!isBedtimeAt(now.toLocalTime())) {
            return Optional.empty();
        }
        LocalDate day = now.toLocalTime().isBefore(config.end()) ? now.toLocalDate() : now.toLocalDate().plusDays(1);
        return Optional.of(day.atTime(config.end()));
    }

    /** The time bedtime ends, for texts like "You can play again at 7 am". */
    public LocalTime endTime() {
        return config.end();
    }

    /**
     * Returns true once per night while it is bedtime: the UI then shows "Goodnight, little
     * fish!". 01:00 belongs to the night that started the evening before.
     */
    public boolean consumeGoodnight() {
        LocalDateTime now = clock.now();
        if (!isBedtimeAt(now.toLocalTime())) {
            return false;
        }
        LocalDate night = now.toLocalTime().isBefore(config.end()) ? now.toLocalDate().minusDays(1) : now.toLocalDate();
        if (night.equals(goodnightShownFor)) {
            return false;
        }
        goodnightShownFor = night;
        onChange.run();
        return true;
    }

    /**
     * Returns true once per day between {@code end} (07:00) and {@code morningGreetingUntil}
     * (12:00): the UI then shows "Good morning, little fish!".
     */
    public boolean consumeGoodMorning() {
        LocalDateTime now = clock.now();
        LocalTime time = now.toLocalTime();
        if (time.isBefore(config.end()) || !time.isBefore(config.morningGreetingUntil())) {
            return false;
        }
        if (now.toLocalDate().equals(goodMorningShownFor)) {
            return false;
        }
        goodMorningShownFor = now.toLocalDate();
        onChange.run();
        return true;
    }

    /** Publishes {@link BedtimeStarted} or {@link BedtimeEnded} when bedtime begins or ends. Called every frame. */
    public void checkTransition() {
        boolean now = isBedtime();
        if (now != wasBedtime) {
            wasBedtime = now;
            events.publish(now ? new BedtimeStarted() : new BedtimeEnded());
        }
    }

    /**
     * Developer tools only: forgets that tonight's and today's greetings were shown and makes the
     * next {@link #checkTransition()} publish the event that fits the current time, so the
     * matching banner appears at once after the dev clock was moved (21:00 or 07:00).
     */
    public void replayGreetingForDevelopment() {
        goodnightShownFor = null;
        goodMorningShownFor = null;
        wasBedtime = !isBedtime();
        onChange.run();
    }

    /** For the save. */
    public Optional<LocalDate> goodnightShownFor() {
        return Optional.ofNullable(goodnightShownFor);
    }

    /** For the save. */
    public Optional<LocalDate> goodMorningShownFor() {
        return Optional.ofNullable(goodMorningShownFor);
    }
}
