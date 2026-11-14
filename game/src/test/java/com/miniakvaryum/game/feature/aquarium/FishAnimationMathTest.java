package com.miniakvaryum.game.feature.aquarium;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;
import org.junit.jupiter.api.Test;

/** Tests for the animation math (specification section 10.4). They need no window. */
class FishAnimationMathTest {

    private static final float TOLERANCE = 0.001f;

    @Test
    void everyActionHasItsSwimFrequency() {
        assertThat(FishAnimationMath.swimFrequencyHz(ActionType.IDLE)).isEqualTo(0.8f);
        assertThat(FishAnimationMath.swimFrequencyHz(ActionType.SLOW_MOVE)).isEqualTo(1.5f);
        assertThat(FishAnimationMath.swimFrequencyHz(ActionType.DASH)).isEqualTo(3.0f);
        assertThat(FishAnimationMath.swimFrequencyHz(ActionType.EAT)).isEqualTo(1.0f);
    }

    @Test
    void tailSwingsBetweenMinusAndPlusTwentyTwoDegrees() {
        // At 1 Hz one period lasts one second.
        assertThat(FishAnimationMath.tailAngleDeg(1f, 0f)).isCloseTo(0f, within(TOLERANCE));
        assertThat(FishAnimationMath.tailAngleDeg(1f, 0.25f)).isCloseTo(22f, within(TOLERANCE));
        assertThat(FishAnimationMath.tailAngleDeg(1f, 0.5f)).isCloseTo(0f, within(TOLERANCE));
        assertThat(FishAnimationMath.tailAngleDeg(1f, 0.75f)).isCloseTo(-22f, within(TOLERANCE));
    }

    @Test
    void finSwingsBetweenMinusAndPlusTwelveDegreesAQuarterPeriodAhead() {
        assertThat(FishAnimationMath.finAngleDeg(1f, 0f)).isCloseTo(12f, within(TOLERANCE));
        assertThat(FishAnimationMath.finAngleDeg(1f, 0.25f)).isCloseTo(0f, within(TOLERANCE));
        assertThat(FishAnimationMath.finAngleDeg(1f, 0.5f)).isCloseTo(-12f, within(TOLERANCE));

        // "A quarter period ahead": when the tail is at its middle position, the fin is at its maximum.
        assertThat(FishAnimationMath.tailAngleDeg(1f, 0f)).isCloseTo(0f, within(TOLERANCE));
        assertThat(FishAnimationMath.finAngleDeg(1f, 0f)).isCloseTo(12f, within(TOLERANCE));
    }
}