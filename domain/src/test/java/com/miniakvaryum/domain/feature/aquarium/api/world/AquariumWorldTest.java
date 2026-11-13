package com.miniakvaryum.domain.feature.aquarium.api.world;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for the simulation loop (specification section 7.9).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. Needs FoodField, FoodService and FishBrain.
 */
@Disabled("TODO: enable when AquariumWorld is implemented")
class AquariumWorldTest {

    @Test
    void updateAdvancesPelletsAndFish() {
        fail("TODO: write this test");
    }

    @Test
    void longFramesAreCutToTheMaximumStep() {
        // update(1.0) ages the fish by 1/15 s, not by 1 s.
        fail("TODO: write this test");
    }

    @Test
    void updateOrderRotatesSoEveryFishGetsTheFirstTurn() {
        fail("TODO: write this test");
    }

    @Test
    void everyFishGetsOlderByTheFrameTime() {
        fail("TODO: write this test");
    }

    @Test
    void threeFishAndOnePelletEndWithExactlyOneFeeding() {
        // Run the world until the pellet is gone: the sum of all feed counts is 1, food went down by 1.
        fail("TODO: write this test");
    }

    @Test
    void dropPelletPutsAPelletIntoTheWater() {
        fail("TODO: write this test");
    }

    @Test
    void dropPelletWithoutFoodReturnsEmpty() {
        fail("TODO: write this test");
    }
}
