package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.core.util.Vec2;
import java.util.Objects;

/**
 * Where a fish is and what it is doing right now. Runtime only: never saved, rebuilt at start-up.
 *
 * <p>Everybody may read it (the renderer does, every frame). Only classes in this package can
 * change it, which in practice means {@link FishBrain}.
 */
public final class FishState {

    private Vec2 position;
    private int facing = 1;
    private ActionType action = ActionType.IDLE;
    private double remainingS;
    private Vec2 velocity = Vec2.ZERO;
    private String claimedPelletId;

    FishState(Vec2 position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    /** Center of the fish in world units. */
    public Vec2 position() {
        return position;
    }

    /** +1 if the fish faces right, -1 if it faces left. Changes only when an action starts. */
    public int facing() {
        return facing;
    }

    /** The action in progress. */
    public ActionType action() {
        return action;
    }

    /** Seconds until the current action ends; 0 or less means the fish must choose a new action. */
    public double remainingS() {
        return remainingS;
    }

    /** Movement in units per second; zero for IDLE and EAT. */
    public Vec2 velocity() {
        return velocity;
    }

    /** Id of the pellet this fish has claimed for eating, or null. */
    public String claimedPelletId() {
        return claimedPelletId;
    }

    void setPosition(Vec2 position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    void setFacing(int facing) {
        if (facing != 1 && facing != -1) {
            throw new IllegalArgumentException("facing must be +1 or -1 but was " + facing);
        }
        this.facing = facing;
    }

    void setAction(ActionType action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    void setRemainingS(double remainingS) {
        this.remainingS = remainingS;
    }

    void setVelocity(Vec2 velocity) {
        this.velocity = Objects.requireNonNull(velocity, "velocity");
    }

    void setClaimedPelletId(String claimedPelletId) {
        this.claimedPelletId = claimedPelletId;
    }
}
