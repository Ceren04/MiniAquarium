package com.miniakvaryum.domain.feature.aquarium.api.food;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for pellets and claims (specification section 7.8).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. This test class is in the same package as
 * {@link FoodField}, so it can call {@code field.add(id, position)} directly.
 */
@Disabled("TODO: enable when FoodField is implemented")
class FoodFieldTest {

    @Test
    void nearestFreeReturnsTheClosestFreePellet() {
        fail("TODO: write this test");
    }

    @Test
    void nearestFreeIgnoresClaimedPellets() {
        fail("TODO: write this test");
    }

    @Test
    void nearestFreeIsEmptyWithoutFreePellets() {
        fail("TODO: write this test");
    }

    @Test
    void firstClaimSucceedsAndSecondClaimFails() {
        fail("TODO: write this test");
    }

    @Test
    void claimOfUnknownPelletFails() {
        fail("TODO: write this test");
    }

    @Test
    void releaseByTheOwnerFreesThePellet() {
        fail("TODO: write this test");
    }

    @Test
    void releaseBySomeoneElseDoesNothing() {
        fail("TODO: write this test");
    }

    @Test
    void removeTakesThePelletOutOfTheWater() {
        fail("TODO: write this test");
    }

    @Test
    void pelletSinksAndStopsAboveTheFloor() {
        // Stops at waterFloor + 10.
        fail("TODO: write this test");
    }

    @Test
    void uneatenPelletDissolvesAfterItsLifetime() {
        fail("TODO: write this test");
    }

    @Test
    void claimedPelletDoesNotDissolve() {
        fail("TODO: write this test");
    }

    @Test
    void claimTimeoutMakesThePelletFreeAgain() {
        fail("TODO: write this test");
    }
}
