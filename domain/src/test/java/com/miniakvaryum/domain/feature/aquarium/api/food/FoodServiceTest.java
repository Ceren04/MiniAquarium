package com.miniakvaryum.domain.feature.aquarium.api.food;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for feeding and food accounting (specification section 7.8).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. Start a game with
 * {@code new TestGame().start(new EconomyFeature(...), new AquariumFeature(...))} and get
 * {@code FoodService}, {@code AquariumWorld} and {@code Wallet} from the session.
 */
@Disabled("TODO: enable when FoodService is implemented")
class FoodServiceTest {

    @Test
    void availableIsStockMinusPelletsInTheWater() {
        // 5 food, 2 pellets dropped (one of them claimed): available is 3.
        fail("TODO: write this test");
    }

    @Test
    void dropCreatesAFreePelletAtTheStartHeight() {
        fail("TODO: write this test");
    }

    @Test
    void dropClampsXIntoTheSwimArea() {
        fail("TODO: write this test");
    }

    @Test
    void dropDoesNotChangeTheWallet() {
        fail("TODO: write this test");
    }

    @Test
    void dropIsRefusedWhenNothingIsAvailable() {
        // Also when the stock is 5 and 5 pellets are already in the water.
        fail("TODO: write this test");
    }

    @Test
    void stockDecreasesOnlyOnCompleteEating() {
        fail("TODO: write this test");
    }

    @Test
    void completeEatingCountsTheFeedingAndRemovesThePellet() {
        fail("TODO: write this test");
    }

    @Test
    void completeEatingTwicePaysOnce() {
        // Second call returns CLAIM_LOST.
        fail("TODO: write this test");
    }

    @Test
    void completeEatingWithoutClaimChangesNothing() {
        fail("TODO: write this test");
    }

    @Test
    void completeEatingWithEmptyStockReleasesThePellet() {
        // Returns INSUFFICIENT_FOOD, feed count unchanged.
        fail("TODO: write this test");
    }

    @Test
    void uneatenPelletDissolvesWithoutCost() {
        fail("TODO: write this test");
    }

    @Test
    void threeFishAndOnePelletGiveExactlyOneEater() {
        fail("TODO: write this test");
    }

    @Test
    void completeEatingPublishesFishFed() {
        fail("TODO: write this test");
    }
}
