package com.miniakvaryum.domain.feature.aquarium.api.food;

import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.FoodConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The pellets that are in the water right now, and the rule that one pellet can be eaten by
 * only one fish: a fish must <b>claim</b> a pellet before it starts eating, and a claimed
 * pellet is invisible to all other fish.
 *
 * <p>Tests: {@code FoodFieldTest}.
 */
public final class FoodField {

    /** Pellets stop sinking this far above the water floor. */
    private static final double FLOOR_OFFSET = 10;

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
     * Claimed pellets are never returned. With two equally distant pellets the older one wins,
     * so the result is deterministic.
     */
    public Optional<Pellet> nearestFree(Vec2 from) {
        Pellet nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Pellet pellet : pellets) {
            if (pellet.state() != PelletState.FREE) {
                continue;
            }
            double distance = pellet.position().distanceTo(from);
            if (distance < nearestDistance) {
                nearest = pellet;
                nearestDistance = distance;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /**
     * A fish tries to reserve a pellet for eating. Succeeds only if the pellet exists and is FREE.
     *
     * @return true if the pellet now belongs to this fish
     */
    public boolean tryClaim(String pelletId, String fishId) {
        Optional<Pellet> found = find(pelletId);
        if (found.isEmpty() || found.get().state() != PelletState.FREE) {
            return false;
        }
        Pellet pellet = found.get();
        pellet.setState(PelletState.CLAIMED);
        pellet.setClaimedBy(fishId);
        pellet.setClaimAgeS(0);
        return true;
    }

    /** Gives a claimed pellet back, but only if it is CLAIMED by exactly this fish. */
    public void release(String pelletId, String fishId) {
        Optional<Pellet> found = find(pelletId);
        if (found.isPresent()
                && found.get().state() == PelletState.CLAIMED
                && fishId != null
                && fishId.equals(found.get().claimedBy())) {
            makeFree(found.get());
        }
    }

    /** Takes a pellet out of the water (it was eaten). Unknown ids are ignored. */
    public void remove(String pelletId) {
        pellets.removeIf(pellet -> pellet.id().equals(pelletId));
    }

    /**
     * Advances all pellets by one frame: sink, age, dissolve (FREE pellets only, costs no food)
     * and release claims that have timed out.
     */
    public void update(double dtSeconds) {
        FoodConfig food = config.food();
        double floorY = config.world().waterFloor() + FLOOR_OFFSET;

        Iterator<Pellet> iterator = pellets.iterator();
        while (iterator.hasNext()) {
            Pellet pellet = iterator.next();
            sink(pellet, food.pelletSinkSpeed() * dtSeconds, floorY);
            pellet.setAgeS(pellet.ageS() + dtSeconds);

            if (pellet.state() == PelletState.CLAIMED) {
                pellet.setClaimAgeS(pellet.claimAgeS() + dtSeconds);
                if (pellet.claimAgeS() > food.claimTimeoutS()) {
                    makeFree(pellet);
                }
            } else if (pellet.ageS() > food.pelletLifetimeS()) {
                iterator.remove();
            }
        }
    }

    private static void sink(Pellet pellet, double distance, double floorY) {
        double y = Math.max(floorY, pellet.position().y() - distance);
        pellet.setPosition(new Vec2(pellet.position().x(), y));
    }

    private static void makeFree(Pellet pellet) {
        pellet.setState(PelletState.FREE);
        pellet.setClaimedBy(null);
        pellet.setClaimAgeS(0);
    }

    private Optional<Pellet> find(String pelletId) {
        for (Pellet pellet : pellets) {
            if (pellet.id().equals(pelletId)) {
                return Optional.of(pellet);
            }
        }
        return Optional.empty();
    }
}