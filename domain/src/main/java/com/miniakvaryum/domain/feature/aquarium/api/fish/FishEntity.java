package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.core.util.Vec2;
import java.util.Objects;

/**
 * One fish: its saved facts (id, name, feed count, age) plus its runtime {@link FishState}.
 *
 * <p>The facts change only through the two methods {@link #recordFeeding()} and
 * {@link #addAquariumMillis(long)}.
 */
public final class FishEntity {

    private final String id;
    private final String name;
    private int feedCount;
    private long aquariumMillis;
    private final FishState state;

    /**
     * @param data          the saved facts
     * @param startPosition where the fish appears; positions are not saved, so the caller picks one
     */
    public FishEntity(FishData data, Vec2 startPosition) {
        Objects.requireNonNull(data, "data");
        this.id = Objects.requireNonNull(data.id(), "id");
        this.name = data.name();
        this.feedCount = data.feedCount();
        this.aquariumMillis = data.aquariumMillis();
        this.state = new FishState(startPosition);
    }

    public String id() {
        return id;
    }

    /** Name given by the child, or null. */
    public String name() {
        return name;
    }

    /** How many pellets this fish has eaten. */
    public int feedCount() {
        return feedCount;
    }

    /** How long this fish has lived in the aquarium, in game milliseconds. */
    public long aquariumMillis() {
        return aquariumMillis;
    }

    /** Runtime state: position, action, facing. */
    public FishState state() {
        return state;
    }

    /** Returns the facts to save. */
    public FishData toData() {
        return new FishData(id, name, feedCount, aquariumMillis);
    }

    /** The fish finished eating a pellet: count it and let go of the claim. */
    public void recordFeeding() {
        feedCount++;
        state.setClaimedPelletId(null);
    }

    /**
     * The fish got older.
     *
     * @param millis time to add, not negative
     */
    public void addAquariumMillis(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("millis must be >= 0 but was " + millis);
        }
        aquariumMillis += millis;
    }
}
