package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.core.util.Vec2;

/**
 * Test helper: builds fish in an exact state. It lives in the same package as
 * {@link FishState}, so it may use the setters that production code outside this package
 * cannot reach.
 */
public final class Fishes {

    private Fishes() {
    }

    /** A new, unnamed, idle fish at the given position. */
    public static FishEntity at(String id, double x, double y) {
        return new FishEntity(new FishData(id, null, 0, 0), new Vec2(x, y));
    }

    /** A fish with the given facts at the given position. */
    public static FishEntity of(FishData data, double x, double y) {
        return new FishEntity(data, new Vec2(x, y));
    }

    /** Puts the fish into the middle of an EAT action on the given pellet. */
    public static FishEntity eating(FishEntity fish, String pelletId, double remainingS) {
        fish.state().setAction(ActionType.EAT);
        fish.state().setVelocity(Vec2.ZERO);
        fish.state().setRemainingS(remainingS);
        fish.state().setClaimedPelletId(pelletId);
        return fish;
    }

    /** Puts the fish into the middle of a movement. */
    public static FishEntity moving(FishEntity fish, ActionType action, Vec2 velocity, double remainingS) {
        fish.state().setAction(action);
        fish.state().setVelocity(velocity);
        fish.state().setRemainingS(remainingS);
        return fish;
    }

    /** Sets the direction the fish faces (+1 right, -1 left). */
    public static FishEntity facing(FishEntity fish, int facing) {
        fish.state().setFacing(facing);
        return fish;
    }
}
