package com.miniakvaryum.domain.feature.aquarium.api.fish;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for fish behavior (specification section 7.7).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. Build fish with {@link Fishes}, script the random numbers
 * with {@code ScriptedRandom} and put pellets into a {@code FoodField} through a
 * {@code FoodService} (so {@code FoodField} must be implemented first).
 */
@Disabled("TODO: enable when FishBrain is implemented")
class FishBrainTest {

    @Test
    void runningActionMovesTheFishAndCountsDown() {
        fail("TODO: write this test");
    }

    @Test
    void positionIsClampedAtTheWorldEdges() {
        fail("TODO: write this test");
    }

    @Test
    void weightsAreRespectedInNormalContext() {
        // Seeded JavaRandomSource, 10 000 draws: SLOW_MOVE 40 %, DASH 20 %, IDLE 40 %, each within 3 %.
        fail("TODO: write this test");
    }

    @Test
    void weightsAreRespectedWhenFoodIsDetected() {
        // 3 : 2 : 1 for SLOW_MOVE : DASH : IDLE.
        fail("TODO: write this test");
    }

    @Test
    void weightsAreRespectedInEatingRange() {
        // 1 : 4 for IDLE : EAT.
        fail("TODO: write this test");
    }

    @Test
    void contextSwitchesExactlyAtTheRangeBorders() {
        // Distance 70 is eating range, just above is detected; 300 is detected, just above is normal.
        fail("TODO: write this test");
    }

    @Test
    void claimedPelletsAreIgnoredWhenChoosing() {
        fail("TODO: write this test");
    }

    @Test
    void moveGoesTowardThePelletWhenFoodIsDetected() {
        fail("TODO: write this test");
    }

    @Test
    void moveInNormalContextUsesTheRandomAngle() {
        fail("TODO: write this test");
    }

    @Test
    void directionIsMirroredWhenTheEndPointWouldLeaveTheArea() {
        fail("TODO: write this test");
    }

    @Test
    void facingFollowsTheHorizontalDirectionAtActionStart() {
        fail("TODO: write this test");
    }

    @Test
    void facingStaysWhenMovingAlmostVertically() {
        // abs(dir.x) <= 0.001 keeps the old facing.
        fail("TODO: write this test");
    }

    @Test
    void facingNeverChangesWhileAnActionRuns() {
        fail("TODO: write this test");
    }

    @Test
    void idleDurationIsBetweenMinAndMax() {
        fail("TODO: write this test");
    }

    @Test
    void idleFlipsFacingWithThirtyPercentChance() {
        // Scripted 0.29 flips, 0.30 does not.
        fail("TODO: write this test");
    }

    @Test
    void eatClaimsThePelletAndFacesIt() {
        fail("TODO: write this test");
    }

    @Test
    void failedClaimFallsBackToIdle() {
        fail("TODO: write this test");
    }

    @Test
    void finishedEatingIsReturnedExactlyOnce() {
        fail("TODO: write this test");
    }
}
