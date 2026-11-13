package com.miniakvaryum.game.feature.aquarium;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for the animation math (specification section 8.4). They need no window.
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test.
 */
@Disabled("TODO: enable when FishAnimationMath is implemented")
class FishAnimationMathTest {

    @Test
    void everyActionHasItsSwimFrequency() {
        // IDLE 0.8, SLOW_MOVE 1.5, DASH 3.0, EAT 1.0.
        fail("TODO: write this test");
    }

    @Test
    void tailSwingsBetweenMinusAndPlusTwentyTwoDegrees() {
        // At t = 0 the angle is 0; a quarter period later it is +22; three quarters later -22.
        fail("TODO: write this test");
    }

    @Test
    void finSwingsBetweenMinusAndPlusTwelveDegreesAQuarterPeriodAhead() {
        // At t = 0 the fin is at +12 (phase shift of PI / 2).
        fail("TODO: write this test");
    }
}
