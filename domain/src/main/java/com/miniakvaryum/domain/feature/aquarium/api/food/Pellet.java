package com.miniakvaryum.domain.feature.aquarium.api.food;

import com.miniakvaryum.domain.core.util.Vec2;
import java.util.Objects;

/**
 * One piece of food in the water. Runtime only: pellets are never saved.
 *
 * <p>Everybody may read it; only classes in this package ({@link FoodField}) can change it.
 */
public final class Pellet {

    private final String id;
    private Vec2 position;
    private PelletState state = PelletState.FREE;
    private String claimedBy;
    private double ageS;
    private double claimAgeS;

    Pellet(String id, Vec2 position) {
        this.id = Objects.requireNonNull(id, "id");
        this.position = Objects.requireNonNull(position, "position");
    }

    public String id() {
        return id;
    }

    /** Center of the pellet in world units. */
    public Vec2 position() {
        return position;
    }

    public PelletState state() {
        return state;
    }

    /** Id of the fish that claimed this pellet, or null while it is FREE. */
    public String claimedBy() {
        return claimedBy;
    }

    /** Seconds since the pellet was dropped. */
    public double ageS() {
        return ageS;
    }

    /** Seconds since the pellet was claimed; 0 while it is FREE. */
    public double claimAgeS() {
        return claimAgeS;
    }

    void setPosition(Vec2 position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    void setState(PelletState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    void setAgeS(double ageS) {
        this.ageS = ageS;
    }

    void setClaimAgeS(double claimAgeS) {
        this.claimAgeS = claimAgeS;
    }
}
