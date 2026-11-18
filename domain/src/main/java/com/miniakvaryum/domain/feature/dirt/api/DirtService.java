package com.miniakvaryum.domain.feature.dirt.api;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.core.util.RandomSource;
import com.miniakvaryum.domain.feature.dirt.DirtConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Dirt on the aquarium glass: spots appear over time and the child wipes them away.
 *
 * <p><b>Getting dirty.</b> {@link #advance} adds game time. Every {@code spotIntervalMillis} a
 * new spot appears at a random place, until {@code maxSpots} are on the glass. While the glass
 * is full, time is not banked: the next spot comes a full interval after a spot is removed.
 *
 * <p><b>Getting clean.</b> {@link #wipe} reduces the strength of every spot under the finger in
 * proportion to the rubbing distance. A spot at strength 0 disappears. When the last spot
 * disappears the aquarium counts as cleaned: the game time of the cleaning is stored, the
 * interval starts again from 0 (the next spot comes a full interval after cleaning), and
 * {@link AquariumCleaned} is published.
 *
 * <p>Saving: {@code onChange} is called when a spot appears or disappears. Partial wiping is
 * not worth a save per frame; it is kept by the next autosave.
 */
public final class DirtService {

    /** Strength below which a spot counts as gone (absorbs rounding errors of repeated wipes). */
    private static final double GONE = 1e-9;

    private final DirtConfig config;
    private final RandomSource random;
    private final IdGenerator ids;
    private final EventBus events;
    private final Runnable onChange;
    private final LongSupplier gameTimeNow;
    private final List<DirtSpot> spots;
    private final List<DirtSpot> readOnlySpots;
    private long progressMillis;
    private long lastCleanedGameMillis;

    /**
     * @param spots          saved spots (an empty list for a new game)
     * @param progressMillis saved game time since the last spot appeared or the last cleaning
     * @param lastCleanedGameMillis saved game time of the last cleaning, 0 if never cleaned
     * @param config         dirt rules
     * @param random         places and shapes new spots
     * @param ids            creates spot ids
     * @param events         where {@link AquariumCleaned} is published
     * @param onChange       called when a spot appears or disappears
     * @param gameTimeNow    trusted game time, read when the aquarium is cleaned
     */
    public DirtService(List<DirtSpot> spots, long progressMillis, long lastCleanedGameMillis, DirtConfig config,
            RandomSource random, IdGenerator ids, EventBus events, Runnable onChange, LongSupplier gameTimeNow) {
        if (progressMillis < 0 || lastCleanedGameMillis < 0) {
            throw new IllegalArgumentException("progressMillis and lastCleanedGameMillis must be >= 0");
        }
        this.lastCleanedGameMillis = lastCleanedGameMillis;
        this.gameTimeNow = Objects.requireNonNull(gameTimeNow, "gameTimeNow");
        this.spots = new ArrayList<>(Objects.requireNonNull(spots, "spots"));
        this.readOnlySpots = Collections.unmodifiableList(this.spots);
        this.progressMillis = progressMillis;
        this.config = Objects.requireNonNull(config, "config");
        this.random = Objects.requireNonNull(random, "random");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.events = Objects.requireNonNull(events, "events");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /** Returns the spots on the glass, oldest first. The list cannot be modified. */
    public List<DirtSpot> spots() {
        return readOnlySpots;
    }

    /**
     * Returns game time since the last spot appeared or the last cleaning, whichever was later
     * (saved, so the interval survives a restart).
     */
    public long progressMillis() {
        return progressMillis;
    }

    /** Returns the game time of the last complete cleaning, or 0 if the aquarium was never cleaned. */
    public long lastCleanedGameMillis() {
        return lastCleanedGameMillis;
    }

    /**
     * Returns the dirt level from 0 (clean) to 100 (as many fresh spots as allowed):
     * the sum of all spot strengths relative to {@code maxSpots}, rounded.
     */
    public int levelPercent() {
        double sum = spots.stream().mapToDouble(DirtSpot::strength).sum();
        return (int) Math.round(100.0 * sum / config.maxSpots());
    }

    /**
     * Lets game time pass: adds new spots for every full interval.
     *
     * @param millis game time that passed, not negative
     */
    public void advance(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis must be >= 0 but was " + millis);
        }
        progressMillis += millis;
        boolean added = false;
        while (progressMillis >= config.spotIntervalMillis() && spots.size() < config.maxSpots()) {
            progressMillis -= config.spotIntervalMillis();
            spots.add(newSpot());
            added = true;
        }
        if (spots.size() >= config.maxSpots()) {
            // The glass is full: do not bank time for later.
            progressMillis = Math.min(progressMillis, config.spotIntervalMillis());
        }
        if (added) {
            onChange.run();
        }
    }

    /** A fresh spot at a random place. Draws x, y, radius, variant, rotation in this order. */
    private DirtSpot newSpot() {
        double x = config.minX() + random.nextDouble() * (config.maxX() - config.minX());
        double y = config.minY() + random.nextDouble() * (config.maxY() - config.minY());
        double radius = config.minRadius() + random.nextDouble() * (config.maxRadius() - config.minRadius());
        int variant = random.nextInt(config.variants());
        double rotation = random.nextDouble() * 360.0;
        return new DirtSpot(ids.next(), x, y, radius, variant, rotation, 1.0);
    }

    /** Returns true if a spot is close enough to the given point to be wiped there. */
    public boolean hasSpotAt(double x, double y) {
        return spots.stream().anyMatch(spot -> reaches(spot, x, y));
    }

    /**
     * The child rubs the glass at the given point.
     *
     * @param x           finger position x in world units
     * @param y           finger position y in world units
     * @param rubDistance how far the finger moved since the last call; use
     *                    {@code config.tapRub()} for a tap. Not negative.
     * @return true if at least one spot was touched
     */
    public boolean wipe(double x, double y, double rubDistance) {
        if (rubDistance < 0) {
            throw new IllegalArgumentException("rubDistance must be >= 0 but was " + rubDistance);
        }
        boolean touched = false;
        boolean removed = false;
        double loss = rubDistance / config.rubPerSpot();
        for (int i = spots.size() - 1; i >= 0; i--) {
            DirtSpot spot = spots.get(i);
            if (!reaches(spot, x, y)) {
                continue;
            }
            touched = true;
            double left = spot.strength() - loss;
            if (left <= GONE) {
                spots.remove(i);
                removed = true;
            } else {
                spots.set(i, spot.withStrength(left));
            }
        }
        if (removed) {
            if (spots.isEmpty()) {
                // Clean glass: the next dirt is measured from this moment.
                lastCleanedGameMillis = gameTimeNow.getAsLong();
                progressMillis = 0;
            }
            onChange.run();
            if (spots.isEmpty()) {
                events.publish(new AquariumCleaned());
            }
        }
        return touched;
    }

    /** A single tap at the given point: wipes as much as {@code tapRub} of rubbing. */
    public boolean tap(double x, double y) {
        return wipe(x, y, config.tapRub());
    }

    private boolean reaches(DirtSpot spot, double x, double y) {
        return Math.hypot(spot.x() - x, spot.y() - y) <= spot.radius() + config.fingerRadius();
    }
}
