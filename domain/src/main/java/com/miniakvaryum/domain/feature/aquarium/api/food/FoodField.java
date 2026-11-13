package com.miniakvaryum.domain.feature.aquarium.api.food;

import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The pellets that are in the water right now, and the rule that one pellet can be eaten by
 * only one fish: a fish must <b>claim</b> a pellet before it starts eating, and a claimed
 * pellet is invisible to all other fish.
 *
 * <p><b>SKELETON:</b> {@link #pellets()} and {@code add} work; the methods marked TODO must be
 * implemented (specification section 7.8). Tests: {@code FoodFieldTest}. Implement this class
 * before {@code FishBrain}, whose tests need it.
 */
public final class FoodField {

    private final AquariumConfig config;
    private final List<Pellet> pellets = new ArrayList<>();
    private final List<Pellet> readOnlyPellets = Collections.unmodifiableList(pellets);

    public FoodField(AquariumConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /** Returns all pellets in the water (FREE and CLAIMED), oldest first. The list cannot be modified. */
    public List<Pellet> pellets() {
        return readOnlyPellets;
    }

    /** Puts a new FREE pellet into the water. Called by {@link FoodService#drop}. */
    Pellet add(String id, Vec2 position) {
        Pellet pellet = new Pellet(id, position);
        pellets.add(pellet);
        return pellet;
    }

    /**
     * Returns the FREE pellet closest to {@code from}, or empty if there is none.
     * Claimed pellets are never returned.
     *
     * <p>TODO.
     */
    public Optional<Pellet> nearestFree(Vec2 from) {
        throw new UnsupportedOperationException("TODO: FoodField.nearestFree is not implemented yet");
    }

    /**
     * A fish tries to reserve a pellet for eating.
     *
     * <p>TODO: succeed only if the pellet exists and is FREE; then set it to CLAIMED by
     * {@code fishId} with {@code claimAgeS = 0}.
     *
     * @return true if the pellet now belongs to this fish
     */
    public boolean tryClaim(String pelletId, String fishId) {
        throw new UnsupportedOperationException("TODO: FoodField.tryClaim is not implemented yet");
    }

    /**
     * Gives a claimed pellet back.
     *
     * <p>TODO: only if the pellet is CLAIMED by exactly this fish, set it back to FREE;
     * otherwise do nothing.
     */
    public void release(String pelletId, String fishId) {
        throw new UnsupportedOperationException("TODO: FoodField.release is not implemented yet");
    }

    /**
     * Takes a pellet out of the water (it was eaten). Unknown ids are ignored.
     *
     * <p>TODO.
     */
    public void remove(String pelletId) {
        throw new UnsupportedOperationException("TODO: FoodField.remove is not implemented yet");
    }

    /**
     * Advances all pellets by one frame.
     *
     * <p>TODO, for every pellet:
     * <ul>
     *   <li>Sink: move down by {@code pelletSinkSpeed * dt}, but not below {@code waterFloor + 10}.</li>
     *   <li>Age: add dt to {@code ageS}; add dt to {@code claimAgeS} while CLAIMED.</li>
     *   <li>Dissolve: remove a FREE pellet older than {@code pelletLifetimeS}. This costs no food.</li>
     *   <li>Claim timeout: a CLAIMED pellet whose {@code claimAgeS} exceeds {@code claimTimeoutS}
     *       becomes FREE again.</li>
     * </ul>
     * Careful: do not remove from the list while iterating over it with a for-each loop.
     */
    public void update(double dtSeconds) {
        throw new UnsupportedOperationException("TODO: FoodField.update is not implemented yet");
    }
}
