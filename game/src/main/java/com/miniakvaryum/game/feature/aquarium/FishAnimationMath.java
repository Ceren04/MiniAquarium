package com.miniakvaryum.game.feature.aquarium;

import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;

/**
 * The pure math of the fish animation. It uses no libGDX class, so it can be unit tested
 * without a window (see {@code FishAnimationMathTest}).
 */
final class FishAnimationMath {

    private static final float TAIL_AMPLITUDE_DEG = 22f;
    private static final float FIN_AMPLITUDE_DEG = 12f;

    private FishAnimationMath() {
    }

    /** Returns how often tail and fin swing per second for the given action. */
    static float swimFrequencyHz(ActionType action) {
        return switch (action) {
            case IDLE -> 0.8f;
            case SLOW_MOVE -> 1.5f;
            case DASH -> 3.0f;
            case EAT -> 1.0f;
        };
    }

    /**
     * Returns the rotation of the tail in degrees: {@code 22 * sin(2 * PI * f * t)}.
     *
     * @param timeSeconds running time of this fish's animation
     */
    static float tailAngleDeg(float frequencyHz, float timeSeconds) {
        return (float) (TAIL_AMPLITUDE_DEG * Math.sin(2 * Math.PI * frequencyHz * timeSeconds));
    }

    /**
     * Returns the rotation of the fin in degrees: {@code 12 * sin(2 * PI * f * t + PI / 2)}.
     * The fin is a quarter swing ahead of the tail.
     */
    static float finAngleDeg(float frequencyHz, float timeSeconds) {
        return (float) (FIN_AMPLITUDE_DEG * Math.sin(2 * Math.PI * frequencyHz * timeSeconds + Math.PI / 2));
    }
}