package com.miniakvaryum.game.feature.aquarium;

import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;

/**
 * The pure math of the fish animation. It uses no libGDX class, so it can be unit tested
 * without a window (see {@code FishAnimationMathTest}).
 *
 * <p><b>SKELETON</b> (specification section 8.4).
 */
final class FishAnimationMath {

    private FishAnimationMath() {
    }

    /**
     * Returns how often tail and fin swing per second for the given action.
     *
     * <p>TODO: IDLE 0.8, SLOW_MOVE 1.5, DASH 3.0, EAT 1.0.
     */
    static float swimFrequencyHz(ActionType action) {
        throw new UnsupportedOperationException("TODO: FishAnimationMath.swimFrequencyHz is not implemented yet");
    }

    /**
     * Returns the rotation of the tail in degrees.
     *
     * <p>TODO: {@code 22 * sin(2 * PI * frequencyHz * timeSeconds)}.
     *
     * @param timeSeconds running time of this fish's animation
     */
    static float tailAngleDeg(float frequencyHz, float timeSeconds) {
        throw new UnsupportedOperationException("TODO: FishAnimationMath.tailAngleDeg is not implemented yet");
    }

    /**
     * Returns the rotation of the fin in degrees.
     *
     * <p>TODO: {@code 12 * sin(2 * PI * frequencyHz * timeSeconds + PI / 2)}.
     */
    static float finAngleDeg(float frequencyHz, float timeSeconds) {
        throw new UnsupportedOperationException("TODO: FishAnimationMath.finAngleDeg is not implemented yet");
    }
}
